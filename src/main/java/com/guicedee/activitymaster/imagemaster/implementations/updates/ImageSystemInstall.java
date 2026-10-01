package com.guicedee.activitymaster.imagemaster.implementations.updates;

import com.guicedee.activitymaster.fsdm.client.services.IResourceItemService;
import com.guicedee.activitymaster.fsdm.client.services.IActivityMasterService;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.enterprise.IEnterprise;
import com.guicedee.activitymaster.fsdm.client.services.systems.ISystemUpdate;
import com.guicedee.activitymaster.fsdm.client.services.systems.SortedUpdate;
import com.guicedee.activitymaster.imagemaster.services.IImageService;
import com.guicedee.activitymaster.imagemaster.implementations.ImageSystem;
import io.smallrye.mutiny.Uni;
import lombok.extern.log4j.Log4j2;
import org.hibernate.reactive.mutiny.Mutiny;

import static com.guicedee.client.IGuiceContext.get;

/**
 * Creates only the resource-item-type taxonomy for the Image System at install time. Image binaries
 * themselves are stored on demand via the service / REST endpoints — never at startup.
 */
@SortedUpdate(sortOrder = 1100, taskCount = 1)
@Log4j2
public class ImageSystemInstall implements ISystemUpdate
{
	/** Taxonomy and the install receipt share the caller's stateless transaction. */
	@Override
	public Uni<Boolean> update(Mutiny.StatelessSession session, IEnterprise<?, ?> enterprise)
	{
		log.info("Starting image system installation");
		ImageSystem imageSystem = get(ImageSystem.class);
		return imageSystem.hasSystemInstalled(session, enterprise)
			.chain(installed -> installed
				? IActivityMasterService.getISystem(session, IImageService.ImageSystemName, enterprise)
				: imageSystem.registerSystem(session, enterprise))
			.chain(system -> IActivityMasterService.getISystemToken(session, IImageService.ImageSystemName, enterprise)
				.chain(token -> {
					logProgress("Image Master", "Creating Image resource item type");
					IResourceItemService<?> resources = get(IResourceItemService.class);
					return resources.createType(session, IImageService.ImageResourceType,
						"Binary image stored and served by the Image Master", system, token).replaceWith(Boolean.TRUE);
				})).onFailure().invoke(e -> log.error("Error during image system installation: {}", e.getMessage(), e))
		  .onItem().invoke(() -> log.info("Image system installation completed successfully"));
	}
}

