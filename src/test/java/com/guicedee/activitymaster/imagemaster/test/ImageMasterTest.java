package com.guicedee.activitymaster.imagemaster.test;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.guicedee.activitymaster.fsdm.client.services.IClassificationService;
import com.guicedee.activitymaster.fsdm.client.services.IEnterpriseService;
import com.guicedee.activitymaster.fsdm.client.services.IResourceItemService;
import com.guicedee.activitymaster.fsdm.client.services.SessionUtils;
import com.guicedee.activitymaster.fsdm.client.services.administration.ActivityMasterConfiguration;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.enterprise.IEnterprise;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.fsdm.client.services.classifications.EnterpriseClassificationDataConcepts;
import com.guicedee.activitymaster.imagemaster.implementations.updates.ImageSystemInstall;
import com.guicedee.activitymaster.imagemaster.rest.ImageRestService;
import com.guicedee.activitymaster.imagemaster.services.IImageService;
import com.guicedee.client.IGuiceContext;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.*;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the Image Master proving images are stored, secured and retrieved as resource
 * items through the service and REST endpoints. Boots the reactive stack against Testcontainers
 * PostgreSQL, exactly like the geography module.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ImageMasterTest
{
	private static final String ENTERPRISE = "ImageTestCo";
	private static final String IMAGE_SYSTEM = IImageService.ImageSystemName;

	private Mutiny.SessionFactory sessionFactory;
	private IImageService<?> imageService;
	private ImageRestService restService;

	@BeforeAll
	public void setup()
	{
		ActivityMasterConfiguration.get().setApplicationEnterpriseName(ENTERPRISE);
		IGuiceContext.instance();

		sessionFactory = IGuiceContext.get(Key.get(Mutiny.SessionFactory.class, Names.named("ActivityMaster-Test")));
		assertNotNull(sessionFactory, "SessionFactory should not be null");

		imageService = IGuiceContext.get(IImageService.class);
		assertNotNull(imageService, "IImageService should be injectable");

		restService = IGuiceContext.get(ImageRestService.class);
		assertNotNull(restService, "ImageRestService should be injectable");

		bootstrapEnterprise();
		installImageSystem();
	}

	private void bootstrapEnterprise()
	{
		sessionFactory.withSession(session -> session.withTransaction(tx -> {
			IEnterpriseService<?> enterpriseService = IGuiceContext.get(IEnterpriseService.class);
			return enterpriseService.getEnterprise(session, ENTERPRISE)
					.onFailure().recoverWithUni(t -> {
						var ent = enterpriseService.get();
						ent.setName(ENTERPRISE);
						ent.setDescription("Image Master test enterprise");
						return enterpriseService.createNewEnterprise(session, ent)
								.chain(e -> enterpriseService.startNewEnterprise(session, ENTERPRISE, "admin", "adminadmin!@"));
					})
					.replaceWith(Uni.createFrom().voidItem());
		})).await().atMost(Duration.ofMinutes(3));
	}

	private void installImageSystem()
	{
		IEnterpriseService<?> enterpriseService = IGuiceContext.get(IEnterpriseService.class);
		IEnterprise<?, ?> enterprise = sessionFactory.withSession(s -> enterpriseService.getEnterprise(s, ENTERPRISE))
				.await().atMost(Duration.ofMinutes(1));
		assertNotNull(enterprise, "Enterprise must exist before installing the image system");

		ImageSystemInstall install = IGuiceContext.get(ImageSystemInstall.class);
		Boolean done = install.update((Mutiny.Session) null, enterprise).await().atMost(Duration.ofMinutes(2));
		assertEquals(Boolean.TRUE, done, "Image system installation should succeed");
	}

	private static byte[] pngBytes(int w, int h, Color color)
	{
		try
		{
			BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = img.createGraphics();
			g.setColor(color);
			g.fillRect(0, 0, w, h);
			g.dispose();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(img, "png", out);
			return out.toByteArray();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	// -------------------------------------------------------------------------------------------
	//  Pure conversion / optimisation (no DB)
	// -------------------------------------------------------------------------------------------

	@Test
	@Order(1)
	@DisplayName("optimize bounds a large image to the requested dimensions, preserving aspect ratio")
	public void optimizeShrinksImage()
	{
		byte[] big = pngBytes(400, 200, Color.BLUE);
		byte[] small = imageService.optimize(big, 100, 100, "png");

		BufferedImage decoded = imageService.toBufferedImage(small);
		assertNotNull(decoded, "Optimised bytes should decode");
		assertTrue(decoded.getWidth() <= 100 && decoded.getHeight() <= 100, "Image must be bounded");
		assertEquals(2.0, (double) decoded.getWidth() / decoded.getHeight(), 0.2, "Aspect ratio preserved");
	}

	@Test
	@Order(2)
	@DisplayName("optimize returns the original bytes when no shrink is required")
	public void optimizeKeepsOriginalWhenSmaller()
	{
		byte[] small = pngBytes(50, 50, Color.RED);
		assertArrayEquals(small, imageService.optimize(small, 200, 200, "png"), "No scaling when within bounds");
	}

	// -------------------------------------------------------------------------------------------
	//  Resource-item backed store / retrieve via REST
	// -------------------------------------------------------------------------------------------

	@Test
	@Order(10)
	@DisplayName("upload + get round-trips an image through resource items")
	public void uploadAndGetRoundTrip()
	{
		byte[] original = pngBytes(120, 120, Color.GREEN);
		String id = restService.upload(ENTERPRISE, IMAGE_SYSTEM, "round-trip", original)
				.await().atMost(Duration.ofMinutes(1));
		assertNotNull(id, "upload should return an id");

		Response response = restService.getImage(ENTERPRISE, IMAGE_SYSTEM, id, 0, 0)
				.await().atMost(Duration.ofMinutes(1));
		assertEquals(200, response.getStatus(), "Stored image should be retrievable");
		assertArrayEquals(original, (byte[]) response.getEntity(), "Retrieved bytes match stored bytes");
	}

	@Test
	@Order(20)
	@DisplayName("get with width/height returns an optimised, bounded copy")
	public void getOptimizedReturnsScaled()
	{
		byte[] original = pngBytes(300, 300, Color.MAGENTA);
		String id = restService.upload(ENTERPRISE, IMAGE_SYSTEM, "scaled", original)
				.await().atMost(Duration.ofMinutes(1));

		Response response = restService.getImage(ENTERPRISE, IMAGE_SYSTEM, id, 64, 64)
				.await().atMost(Duration.ofMinutes(1));
		assertEquals(200, response.getStatus());

		BufferedImage decoded = imageService.toBufferedImage((byte[]) response.getEntity());
		assertNotNull(decoded);
		assertTrue(decoded.getWidth() <= 64 && decoded.getHeight() <= 64, "Optimised image bounded to 64px");
	}

	@Test
	@Order(30)
	@DisplayName("get of an unknown image id returns 404")
	public void getMissingReturns404()
	{
		Response response = restService.getImage(ENTERPRISE, IMAGE_SYSTEM, java.util.UUID.randomUUID().toString(), 0, 0)
				.await().atMost(Duration.ofMinutes(1));
		assertEquals(404, response.getStatus(), "Unknown image should be 404");
	}

	// -------------------------------------------------------------------------------------------
	//  Retrieve by classification / value via REST
	// -------------------------------------------------------------------------------------------

	@Test
	@Order(40)
	@DisplayName("upload + classify + get-by-classification round-trips an image located by classification/value")
	public void getByClassificationRoundTrip()
	{
		final String classification = "ImageTestClassification";
		final String value = "logo-primary";

		// Reference data: create the classification (committed) under the image system scope.
		SessionUtils.<Void>withActivityMaster(ENTERPRISE, IMAGE_SYSTEM, tuple -> {
			Mutiny.Session session = tuple.getItem1();
			ISystems<?, ?> system = tuple.getItem3();
			IClassificationService<?> classificationService = IGuiceContext.get(IClassificationService.class);
			return classificationService.create(session, classification, "Image master test classification",
							EnterpriseClassificationDataConcepts.NoClassificationDataConceptName, system, tuple.getItem4())
					.replaceWithVoid();
		}).await().atMost(Duration.ofMinutes(2));

		// Store the image (committed by the REST service).
		byte[] original = pngBytes(80, 80, Color.ORANGE);
		String id = restService.upload(ENTERPRISE, IMAGE_SYSTEM, "classified", original)
				.await().atMost(Duration.ofMinutes(1));
		assertNotNull(id, "upload should return an id");

		// Attach the classification value to the uploaded resource item (committed).
		SessionUtils.<Void>withActivityMaster(ENTERPRISE, IMAGE_SYSTEM, tuple -> {
			Mutiny.Session session = tuple.getItem1();
			ISystems<?, ?> system = tuple.getItem3();
			IResourceItemService<?> resourceItemService = IGuiceContext.get(IResourceItemService.class);
			return resourceItemService.findByUUID(session, java.util.UUID.fromString(id))
					.chain(item -> item.addClassification(session, classification, value, system, tuple.getItem4()))
					.replaceWithVoid();
		}).await().atMost(Duration.ofMinutes(2));

		// Locate + render by classification + value.
		Response response = restService.getImageByClassification(ENTERPRISE, IMAGE_SYSTEM, classification, value, 0, 0)
				.await().atMost(Duration.ofMinutes(1));
		assertEquals(200, response.getStatus(), "Image should be retrievable by classification + value");
		assertArrayEquals(original, (byte[]) response.getEntity(), "Retrieved bytes match stored bytes");

		// A blank value matches any value for the classification.
		Response anyValue = restService.getImageByClassification(ENTERPRISE, IMAGE_SYSTEM, classification, "", 0, 0)
				.await().atMost(Duration.ofMinutes(1));
		assertEquals(200, anyValue.getStatus(), "Blank value should match any value for the classification");
		assertArrayEquals(original, (byte[]) anyValue.getEntity(), "Blank-value retrieval returns the same bytes");
	}

	@Test
	@Order(50)
	@DisplayName("get-by-classification with width/height returns an optimised, bounded copy")
	public void getByClassificationOptimized()
	{
		final String classification = "ImageTestClassificationScaled";
		final String value = "banner";

		SessionUtils.<Void>withActivityMaster(ENTERPRISE, IMAGE_SYSTEM, tuple -> {
			Mutiny.Session session = tuple.getItem1();
			ISystems<?, ?> system = tuple.getItem3();
			IClassificationService<?> classificationService = IGuiceContext.get(IClassificationService.class);
			return classificationService.create(session, classification, "Image master scaled test classification",
							EnterpriseClassificationDataConcepts.NoClassificationDataConceptName, system, tuple.getItem4())
					.replaceWithVoid();
		}).await().atMost(Duration.ofMinutes(2));

		byte[] original = pngBytes(300, 300, Color.CYAN);
		String id = restService.upload(ENTERPRISE, IMAGE_SYSTEM, "classified-scaled", original)
				.await().atMost(Duration.ofMinutes(1));

		SessionUtils.<Void>withActivityMaster(ENTERPRISE, IMAGE_SYSTEM, tuple -> {
			Mutiny.Session session = tuple.getItem1();
			ISystems<?, ?> system = tuple.getItem3();
			IResourceItemService<?> resourceItemService = IGuiceContext.get(IResourceItemService.class);
			return resourceItemService.findByUUID(session, java.util.UUID.fromString(id))
					.chain(item -> item.addClassification(session, classification, value, system, tuple.getItem4()))
					.replaceWithVoid();
		}).await().atMost(Duration.ofMinutes(2));

		Response response = restService.getImageByClassification(ENTERPRISE, IMAGE_SYSTEM, classification, value, 64, 64)
				.await().atMost(Duration.ofMinutes(1));
		assertEquals(200, response.getStatus());

		BufferedImage decoded = imageService.toBufferedImage((byte[]) response.getEntity());
		assertNotNull(decoded);
		assertTrue(decoded.getWidth() <= 64 && decoded.getHeight() <= 64, "Optimised image bounded to 64px");
	}

	@Test
	@Order(60)
	@DisplayName("get-by-classification with no match returns 404")
	public void getByClassificationMissingReturns404()
	{
		Response response = restService.getImageByClassification(ENTERPRISE, IMAGE_SYSTEM, "NoSuchClassification", "nope", 0, 0)
				.await().atMost(Duration.ofMinutes(1));
		assertEquals(404, response.getStatus(), "Unknown classification should be 404");
	}
}

