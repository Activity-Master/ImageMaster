import com.guicedee.activitymaster.imagemaster.implementations.ImageServiceBinder;
import com.guicedee.guicedinjection.interfaces.IGuiceModule;

module com.guicedee.activitymaster.imagemaster {
	requires java.desktop;
	requires java.logging;
	exports com.guicedee.activitymaster.imagemaster.services;
	
	requires com.guicedee.guicedservlets;
	
	provides IGuiceModule with ImageServiceBinder;
	

	exports com.guicedee.activitymaster.imagemaster.implementations;
	exports com.guicedee.activitymaster.imagemaster;
	
	
	opens com.guicedee.activitymaster.imagemaster.implementations to com.google.guice;
	opens com.guicedee.activitymaster.imagemaster to com.google.guice;
	opens com.guicedee.activitymaster.imagemaster.services to com.google.guice;
	
}
