package com.guicedee.activitymaster.imagemaster.services;

import java.awt.image.BufferedImage;

public interface IImageService<J extends IImageService<J>>
{
	BufferedImage toBufferedImage(byte[] dataBytes);
}
