package com.guicedee.activitymaster.imagemaster;

import com.guicedee.activitymaster.imagemaster.services.IImageService;
import lombok.extern.log4j.Log4j2;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Log4j2
public class ImageService implements IImageService<ImageService>
{

	@Override
	public BufferedImage toBufferedImage(byte[] dataBytes)
	{
		if (dataBytes == null || dataBytes.length == 0)
		{
			return null;
		}
		try
		{
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(dataBytes));
			return image;
		}
		catch (IOException e)
		{
			log.error("Error reading bytes", e);
			return null;
		}
	}
	
}
