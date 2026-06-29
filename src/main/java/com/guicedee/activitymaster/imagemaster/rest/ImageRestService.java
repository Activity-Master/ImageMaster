package com.guicedee.activitymaster.imagemaster.rest;

import com.google.inject.Inject;
import com.guicedee.activitymaster.fsdm.client.services.SessionUtils;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.imagemaster.services.IImageService;
import io.smallrye.mutiny.Uni;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.log4j.Log4j2;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.UUID;

/**
 * REST surface for the ActivityMaster Image System — secure image upload and optimised retrieval
 * backed by warehouse resource items.
 * <p>
 * Each image is persisted as a binary {@code ResourceItem}, so every read is bounded by the
 * requesting system's security tokens and honours {@code ActiveFlag} row state and token
 * propagation. There is no global image store — an image is only readable inside the security scope
 * that stored it.
 * <p>
 * Retrieval supports on-the-fly bounded scaling via the {@code w}/{@code h} query parameters
 * (Scalr ULTRA_QUALITY, aspect-ratio preserved) for an optimum payload, and responses carry
 * long-lived immutable cache headers. The image media type is sniffed from the magic bytes.
 * <p>
 * All operations run inside the requesting system's security scope via
 * {@link SessionUtils#withActivityMaster}, referencing the owning enterprise and requesting system
 * by name.
 */
@Path("{enterprise}/image")
@Tag(name = "Image", description = "Secure image storage and optimised retrieval backed by security-scoped resource items, with on-the-fly bounded scaling and immutable caching.")
@Log4j2
public class ImageRestService
{
	@Inject
	private IImageService<?> imageService;

	// ──────────────────────────────────────────────────────────────────────────
	// Write — upload
	// ──────────────────────────────────────────────────────────────────────────

	/**
	 * Stores an image and returns its resource item id.
	 */
	@POST
	@Path("{requestingSystemName}/upload")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces(MediaType.TEXT_PLAIN)
	@Operation(operationId = "uploadImage", summary = "Upload an image",
			description = "Stores raw image bytes as a security-scoped resource item and returns its generated resource item id (UUID).")
	@ApiResponse(responseCode = "200", description = "Image stored; body is the resource item id",
			content = @Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(type = "string", format = "uuid")))
	public Uni<String> upload(@Parameter(description = "Owning enterprise name", example = "Acme") @PathParam("enterprise") String enterpriseName,
	                          @Parameter(description = "Requesting system name (security scope)", example = "Image System") @PathParam("requestingSystemName") String systemName,
	                          @Parameter(description = "Logical name for the stored image", example = "logo.png") @QueryParam("name") @DefaultValue("image") String name,
	                          byte[] data)
	{
		return SessionUtils.<String>withActivityMaster(enterpriseName, systemName, tuple -> {
			Mutiny.Session session = tuple.getItem1();
			ISystems<?, ?> system = tuple.getItem3();
			return imageService.storeImage(session, name, data, system, tuple.getItem4())
					.map(UUID::toString);
		}).onFailure().invoke(e -> log.error("Error storing image '{}': {}", name, e.getMessage(), e));
	}

	// ──────────────────────────────────────────────────────────────────────────
	// Read — retrieve (optionally bounded scaling)
	// ──────────────────────────────────────────────────────────────────────────

	/**
	 * Retrieves an image, optionally bounded to {@code w}x{@code h} for an optimum payload.
	 */
	@GET
	@Path("{requestingSystemName}/{imageId}")
	@Produces("image/*")
	@Operation(operationId = "getImage", summary = "Retrieve an image",
			description = "Returns a stored image, honouring row-level security and optional width/height bounded scaling (aspect ratio preserved). The media type is sniffed from the bytes and an immutable cache header is set.")
	@ApiResponse(responseCode = "200", description = "Image found",
			content = @Content(mediaType = "image/*", schema = @Schema(type = "string", format = "binary")))
	@ApiResponse(responseCode = "404", description = "Image not found or not readable in this scope")
	public Uni<Response> getImage(@Parameter(description = "Owning enterprise name", example = "Acme") @PathParam("enterprise") String enterpriseName,
	                              @Parameter(description = "Requesting system name (security scope)", example = "Image System") @PathParam("requestingSystemName") String systemName,
	                              @Parameter(description = "Image resource item id", example = "3f2504e0-4f89-41d3-9a0c-0305e82c3301") @PathParam("imageId") String imageId,
	                              @Parameter(description = "Maximum width in px (0 = original)", example = "256") @QueryParam("w") @DefaultValue("0") int width,
	                              @Parameter(description = "Maximum height in px (0 = original)", example = "256") @QueryParam("h") @DefaultValue("0") int height)
	{
		return SessionUtils.<Response>withActivityMaster(enterpriseName, systemName, tuple -> {
			Mutiny.Session session = tuple.getItem1();
			ISystems<?, ?> system = tuple.getItem3();
			UUID id = UUID.fromString(imageId);
			Uni<byte[]> bytes = (width > 0 || height > 0)
					? imageService.getOptimizedImage(session, id, width, height, system, tuple.getItem4())
					: imageService.getImage(session, id, system, tuple.getItem4());
			return bytes.map(this::toResponse);
		}).onFailure().invoke(e -> log.error("Error retrieving image {}: {}", imageId, e.getMessage(), e));
	}

	// ──────────────────────────────────────────────────────────────────────────
	// Helpers
	// ──────────────────────────────────────────────────────────────────────────

	private Response toResponse(byte[] data)
	{
		if (data == null || data.length == 0)
		{
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		CacheControl cc = new CacheControl();
		cc.setMaxAge(31536000);
		cc.setPrivate(true);
		return Response.ok(data, sniffContentType(data)).cacheControl(cc).build();
	}

	/** Detects the image media type from the magic bytes, falling back to PNG. */
	private String sniffContentType(byte[] data)
	{
		if (data.length >= 3 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF)
		{
			return "image/jpeg";
		}
		if (data.length >= 4 && (data[0] & 0xFF) == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G')
		{
			return "image/png";
		}
		if (data.length >= 6 && data[0] == 'G' && data[1] == 'I' && data[2] == 'F')
		{
			return "image/gif";
		}
		if (data.length >= 2 && data[0] == 'B' && data[1] == 'M')
		{
			return "image/bmp";
		}
		return "image/png";
	}
}






