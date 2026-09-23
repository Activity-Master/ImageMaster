import com.guicedee.activitymaster.fsdm.client.services.systems.IMasterSystem;
import com.guicedee.activitymaster.fsdm.client.services.systems.ISystemUpdate;
import com.guicedee.activitymaster.imagemaster.implementations.ImageMasterModuleInclusion;
import com.guicedee.activitymaster.imagemaster.implementations.ImageServiceBinder;
import com.guicedee.activitymaster.imagemaster.implementations.ImageSystem;
import com.guicedee.activitymaster.imagemaster.implementations.updates.ImageSystemInstall;
import com.guicedee.client.services.config.IGuiceScanModuleInclusions;
import com.guicedee.client.services.lifecycle.IGuiceModule;

module com.guicedee.activitymaster.imagemaster {
	requires java.desktop;

	requires com.guicedee.activitymaster.fsdm;


	requires static lombok;

	provides IGuiceModule with ImageServiceBinder;
	provides IMasterSystem with ImageSystem;
	provides ISystemUpdate with ImageSystemInstall;
	provides IGuiceScanModuleInclusions with ImageMasterModuleInclusion;

	exports com.guicedee.activitymaster.imagemaster;
	exports com.guicedee.activitymaster.imagemaster.services;
	exports com.guicedee.activitymaster.imagemaster.implementations;
	exports com.guicedee.activitymaster.imagemaster.implementations.updates;
	exports com.guicedee.activitymaster.imagemaster.events;
	exports com.guicedee.activitymaster.imagemaster.rest;

	opens com.guicedee.activitymaster.imagemaster to com.google.guice;
	opens com.guicedee.activitymaster.imagemaster.implementations to com.google.guice;
	opens com.guicedee.activitymaster.imagemaster.implementations.updates to com.google.guice;
	opens com.guicedee.activitymaster.imagemaster.services to com.google.guice;
	opens com.guicedee.activitymaster.imagemaster.events to com.google.guice, com.guicedee.vertx;
	opens com.guicedee.activitymaster.imagemaster.rest to com.google.guice, com.guicedee.rest, org.hibernate.reactive, net.bytebuddy;
}
