package com.guicedee.activitymaster.imagemaster.events;

import com.google.inject.Inject;
import com.guicedee.activitymaster.fsdm.client.services.SessionUtils;
import com.guicedee.activitymaster.imagemaster.services.IImageService;
import com.guicedee.vertx.VertxEventDefinition;
import com.guicedee.vertx.VertxEventOptions;
import io.vertx.core.eventbus.Message;
import lombok.extern.log4j.Log4j2;

import java.util.UUID;

import static com.guicedee.activitymaster.fsdm.client.services.administration.ActivityMasterConfiguration.applicationEnterpriseName;
import static com.guicedee.activitymaster.imagemaster.services.IImageService.ImageSystemName;

/**
 * Vert.x event bus consumers for the Image Master.
 *
 * <h3>Event Addresses:</h3>
 * <ul>
 *   <li>{@code image.store} — stores raw image bytes; reply is the resource item id. Body: image bytes.</li>
 * </ul>
 */
@Log4j2
public class ImageEventConsumers
{
	@Inject
	private IImageService<?> imageService;

	/**
	 * Stores image bytes as a secured resource item, replying with the created id.
	 */
	@VertxEventDefinition(value = "image.store", options = @VertxEventOptions(worker = true))
	public String store(Message<byte[]> message)
	{
		byte[] data = message.body();
		return SessionUtils.<UUID>withActivityMaster(applicationEnterpriseName, ImageSystemName, tuple -> {
			return imageService.storeImage(tuple.getItem1(), "image", data, tuple.getItem3(), tuple.getItem4());
		}).map(id -> id.toString())
				.await().indefinitely();
	}
}


