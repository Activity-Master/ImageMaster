package com.guicedee.activitymaster.imagemaster.implementations;

import com.google.inject.*;
import com.guicedee.activitymaster.imagemaster.ImageService;
import com.guicedee.activitymaster.imagemaster.services.IImageService;
import com.guicedee.guicedinjection.interfaces.IGuiceModule;

public class ImageServiceBinder extends PrivateModule
		implements IGuiceModule<ImageServiceBinder>
{
	
	@Override
	protected void configure()
	{
		@SuppressWarnings("Convert2Diamond")
		Key<IImageService<?>> genericKey = Key.get(new TypeLiteral<IImageService<?>>() {});
		@SuppressWarnings("Convert2Diamond")
		Key<IImageService<ImageService>> realKey
				= Key.get(new TypeLiteral<IImageService<ImageService>>() {});
		
		bind(genericKey).to(realKey);
		bind(realKey).to(ImageService.class);
		bind(IImageService.class).to(genericKey);
		
		expose(genericKey);
		expose(IImageService.class);
	}
}
