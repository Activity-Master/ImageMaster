package com.guicedee.activitymaster.imagemaster.implementations.updates;

import com.guicedee.activitymaster.fsdm.client.services.IResourceItemService;
import com.guicedee.activitymaster.fsdm.client.services.SessionUtils;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.enterprise.IEnterprise;
import com.guicedee.activitymaster.fsdm.client.services.systems.ISystemUpdate;
import com.guicedee.activitymaster.fsdm.client.services.systems.SortedUpdate;
import com.guicedee.activitymaster.imagemaster.services.IImageService;
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
	@Override
	public Uni<Boolean> update(Mutiny.Session session, IEnterprise<?, ?> enterprise)
	{
		return doInstall(enterprise);
	}

	/**
	 * Stateless twin of {@link #update(Mutiny.Session, IEnterprise)}. This installer manages its own
	 * ActivityMaster session internally via {@link SessionUtils#withActivityMaster}, so the passed session
	 * (managed or stateless) is not used — both overloads delegate to the same install body.
	 */
	@Override
	public Uni<Boolean> update(Mutiny.StatelessSession session, IEnterprise<?, ?> enterprise)
	{
		return doInstall(enterprise);
	}

	private Uni<Boolean> doInstall(IEnterprise<?, ?> enterprise)
	{
		log.info("Starting image system installation");
		return SessionUtils.<Boolean>withActivityMaster(enterprise.getName(), IImageService.ImageSystemName, tuple -> {
			var amSession = tuple.getItem1();
			var amSystem = tuple.getItem3();
			var amToken = tuple.getItem4();

			logProgress("Image Master", "Creating Image resource item type");
			IResourceItemService<?> resourceItemService = get(IResourceItemService.class);
			return resourceItemService.createType(amSession, IImageService.ImageResourceType,
							"Binary image stored and served by the Image Master", amSystem, amToken)
					.replaceWith(Boolean.TRUE);
		}).onFailure().invoke(e -> log.error("Error during image system installation: {}", e.getMessage(), e))
		  .onItem().invoke(() -> log.info("Image system installation completed successfully"));
	}
}

