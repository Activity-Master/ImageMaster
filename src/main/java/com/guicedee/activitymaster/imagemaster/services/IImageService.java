package com.guicedee.activitymaster.imagemaster.services;

import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;

import java.awt.image.BufferedImage;
import java.util.UUID;

/**
 * Service contract for the Image Master module.
 *
 * <p>Images are stored, secured and retrieved as ActivityMaster <em>resource items</em> so they
 * inherit the full warehouse security matrix (default / scope-restricted), ActiveFlag lifecycle and
 * token propagation. Retrieval is exposed reactively through GuicedEE/Vert.x web
 * ({@code ImageRestService}) with content-type detection, dimension-bounded scaling and
 * cache-friendly responses.</p>
 *
 * @param <J> the concrete implementation type (CRTP)
 */
public interface IImageService<J extends IImageService<J>>
{
	/** The registered system name for images. */
	String ImageSystemName = "Image System";

	/** Resource item type used to store image binaries. */
	String ImageResourceType = "Image";

	/** Default output format used when re-encoding scaled images. */
	String DefaultOutputFormat = "png";

	// ---------------------------------------------------------------------------------------------
	//  In-memory conversions / optimisation
	// ---------------------------------------------------------------------------------------------

	/**
	 * Decodes raw image bytes into a {@link BufferedImage}.
	 *
	 * @param dataBytes the encoded image data
	 * @return the decoded image, or {@code null} when the bytes are empty/undecodable
	 */
	BufferedImage toBufferedImage(byte[] dataBytes);

	/**
	 * Encodes a {@link BufferedImage} back into bytes in the requested format.
	 *
	 * @param image  the source image
	 * @param format the encoder format (e.g. {@code png}, {@code jpg})
	 * @return the encoded bytes, never {@code null}
	 */
	byte[] toBytes(BufferedImage image, String format);

	/**
	 * Produces a scaled, optimised copy of the image bounded to the supplied dimensions while keeping
	 * the original aspect ratio. A non-positive width/height leaves that axis unbounded.
	 *
	 * @param dataBytes the encoded source image
	 * @param width     the maximum width (px), or {@code <=0} for unbounded
	 * @param height    the maximum height (px), or {@code <=0} for unbounded
	 * @param format    the output format (defaults to {@link #DefaultOutputFormat} when blank)
	 * @return the resized, re-encoded bytes — or the original bytes when no scaling is needed
	 */
	byte[] optimize(byte[] dataBytes, int width, int height, String format);

	// ---------------------------------------------------------------------------------------------
	//  Resource-item backed storage & retrieval (secure-by-default)
	// ---------------------------------------------------------------------------------------------

	/**
	 * Stores an image as a secured resource item and returns its id.
	 *
	 * @param session       the active reactive session
	 * @param name          a descriptive name / file name for the image
	 * @param data          the encoded image bytes
	 * @param system        the requesting system (security scope)
	 * @param identityToken optional identity token(s)
	 * @return the created resource item id
	 */
	Uni<UUID> storeImage(Mutiny.Session session, String name, byte[] data, ISystems<?, ?> system, UUID... identityToken);

	/**
	 * Retrieves the raw image bytes for a stored resource item, honouring row-level security.
	 *
	 * @param session       the active reactive session
	 * @param imageId       the resource item id
	 * @param system        the requesting system (security scope)
	 * @param identityToken optional identity token(s)
	 * @return the stored bytes, or {@code null} when not present / not readable
	 */
	Uni<byte[]> getImage(Mutiny.Session session, UUID imageId, ISystems<?, ?> system, UUID... identityToken);

	/**
	 * Retrieves a dimension-bounded, optimised copy of a stored image.
	 *
	 * @param session       the active reactive session
	 * @param imageId       the resource item id
	 * @param width         the maximum width (px), or {@code <=0} for unbounded
	 * @param height        the maximum height (px), or {@code <=0} for unbounded
	 * @param system        the requesting system (security scope)
	 * @param identityToken optional identity token(s)
	 * @return the optimised bytes, or {@code null} when not present / not readable
	 */
	Uni<byte[]> getOptimizedImage(Mutiny.Session session, UUID imageId, int width, int height, ISystems<?, ?> system, UUID... identityToken);
}
