package com.guicedee.activitymaster.imagemaster.implementations;

import com.google.inject.*;
import com.guicedee.activitymaster.imagemaster.ImageService;
import com.guicedee.activitymaster.imagemaster.services.IImageService;
import com.guicedee.client.services.lifecycle.IGuiceModule;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ImageServiceBinder extends AbstractModule
		implements IGuiceModule<ImageServiceBinder>
{
	
	@Override
	protected void configure()
	{
        log.info("Using Image Activity Master Module and routes at /rest/{enterprise}/image");
		@SuppressWarnings("Convert2Diamond")
		Key<IImageService<?>> genericKey = Key.get(new TypeLiteral<IImageService<?>>() {});
		@SuppressWarnings("Convert2Diamond")
		Key<IImageService<ImageService>> realKey
				= Key.get(new TypeLiteral<IImageService<ImageService>>() {});
		
		bind(realKey).to(ImageService.class).in(Singleton.class);
		bind(genericKey).to(realKey);
		bind(IImageService.class).to(genericKey);
	}
}
