package com.guicedee.activitymaster.imagemaster.implementations;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.guicedee.activitymaster.fsdm.client.services.ISystemsService;
import com.guicedee.activitymaster.fsdm.client.services.administration.MasterDefaultSystem;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.enterprise.IEnterprise;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.fsdm.client.services.systems.IMasterSystem;
import com.guicedee.activitymaster.imagemaster.services.IImageService;
import io.smallrye.mutiny.Uni;
import lombok.extern.log4j.Log4j2;
import org.hibernate.reactive.mutiny.Mutiny;

/**
 * Registers the {@code Image System} for an enterprise so that image resource items inherit
 * ActivityMaster security and system context.
 */
@Log4j2
@Singleton
public class ImageSystem
		extends MasterDefaultSystem<ImageSystem>
		implements IMasterSystem<ImageSystem>
{
	@Inject
	private Provider<ISystemsService<?>> systemsService;

	@Override
	public Uni<ISystems<?, ?>> registerSystem(Mutiny.Session session, IEnterprise<?, ?> enterprise)
	{
		log.info("Registering Image System for enterprise: '{}'", enterprise.getName());
		return systemsService.get()
				.create(session, enterprise, getSystemName(), getSystemDescription())
				.chain(system -> getSystem(session, enterprise)
						.chain(sys -> systemsService.get().registerNewSystem(session, enterprise, sys))
						.chain(() -> Uni.createFrom().item(system)));
	}

	@Override
	public Uni<Void> createDefaults(Mutiny.Session session, IEnterprise<?, ?> enterprise)
	{
		return Uni.createFrom().voidItem();
	}

	/** Stateless variant — the image type taxonomy is provisioned by ImageSystemInstall. */
	@Override
	public Uni<Void> createDefaults(Mutiny.StatelessSession session, IEnterprise<?, ?> enterprise)
	{
		return Uni.createFrom().voidItem();
	}

	@Override
	public int totalTasks()
	{
		return 0;
	}

	@Override
	public String getSystemName()
	{
		return IImageService.ImageSystemName;
	}

	@Override
	public String getSystemDescription()
	{
		return "The system for storing, securing and retrieving images";
	}
}
