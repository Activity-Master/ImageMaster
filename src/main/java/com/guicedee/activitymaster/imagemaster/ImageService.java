package com.guicedee.activitymaster.imagemaster;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.guicedee.activitymaster.fsdm.client.services.IResourceItemService;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.resourceitem.IResourceItem;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.imagemaster.services.IImageService;
import com.guicedee.activitymaster.imagemaster.services.Scalr;
import io.smallrye.mutiny.Uni;
import lombok.extern.log4j.Log4j2;
import org.hibernate.reactive.mutiny.Mutiny;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

@Log4j2
public class ImageService implements IImageService<ImageService>
{

	@Inject
	private Provider<IResourceItemService<?>> resourceItemService;

	@Override
	public BufferedImage toBufferedImage(byte[] dataBytes)
	{
		if (dataBytes == null || dataBytes.length == 0)
		{
			return null;
		}
		try
		{
			return ImageIO.read(new ByteArrayInputStream(dataBytes));
		}
		catch (IOException e)
		{
			log.error("Error reading bytes", e);
			return null;
		}
	}

	@Override
	public byte[] toBytes(BufferedImage image, String format)
	{
		if (image == null)
		{
			return new byte[0];
		}
		String fmt = (format == null || format.isBlank()) ? DefaultOutputFormat : format;
		try (ByteArrayOutputStream out = new ByteArrayOutputStream())
		{
			ImageIO.write(image, fmt, out);
			return out.toByteArray();
		}
		catch (IOException e)
		{
			log.error("Error encoding image to {}", fmt, e);
			return new byte[0];
		}
	}

	@Override
	public byte[] optimize(byte[] dataBytes, int width, int height, String format)
	{
		BufferedImage src = toBufferedImage(dataBytes);
		if (src == null)
		{
			return dataBytes;
		}
		int targetW = width > 0 ? width : src.getWidth();
		int targetH = height > 0 ? height : src.getHeight();
		// Nothing to do when the requested bounds already exceed the source dimensions.
		if (targetW >= src.getWidth() && targetH >= src.getHeight())
		{
			return dataBytes;
		}
		BufferedImage scaled = Scalr.resize(src, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.AUTOMATIC, targetW, targetH);
		return toBytes(scaled, format);
	}

	@Override
	public Uni<UUID> storeImage(Mutiny.Session session, String name, byte[] data, ISystems<?, ?> system, UUID... identityToken)
	{
		return resourceItemService.get()
				.create(session, ImageResourceType, name, data, system, identityToken)
				.map(item -> item.getId());
	}

	@Override
	public Uni<byte[]> getImage(Mutiny.Session session, UUID imageId, ISystems<?, ?> system, UUID... identityToken)
	{
		return resourceItemService.get()
				.findByUUID(session, imageId)
				.chain(item -> item == null
						? Uni.createFrom().nullItem()
						: ((IResourceItem<?, ?>) item).getData(session, identityToken))
				// A missing / unreadable resource item surfaces as a not-found query result.
				.onFailure().recoverWithItem((byte[]) null);
	}

	@Override
	public Uni<byte[]> getOptimizedImage(Mutiny.Session session, UUID imageId, int width, int height, ISystems<?, ?> system, UUID... identityToken)
	{
		return getImage(session, imageId, system, identityToken)
				.map(bytes -> bytes == null ? null : optimize(bytes, width, height, DefaultOutputFormat));
	}

	@Override
	public Uni<byte[]> getImageByClassification(Mutiny.Session session, String classification, String value, ISystems<?, ?> system, UUID... identityToken)
	{
		return resourceItemService.get()
				.findByClassification(session, ImageResourceType, classification, value, system, identityToken)
				.chain(item -> item == null
						? Uni.createFrom().nullItem()
						: ((IResourceItem<?, ?>) item).getData(session, identityToken))
				// A missing / unreadable resource item surfaces as a not-found query result.
				.onFailure().recoverWithItem((byte[]) null);
	}

	@Override
	public Uni<byte[]> getOptimizedImageByClassification(Mutiny.Session session, String classification, String value, int width, int height, ISystems<?, ?> system, UUID... identityToken)
	{
		return getImageByClassification(session, classification, value, system, identityToken)
				.map(bytes -> bytes == null ? null : optimize(bytes, width, height, DefaultOutputFormat));
	}

}
