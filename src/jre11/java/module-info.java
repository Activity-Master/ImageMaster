module com.guicedee.activitymaster.imagemaster {
	requires java.desktop;
	requires java.logging;
	exports com.guicedee.activitymaster.imagemaster.services;
	
	requires com.guicedee.guicedservlets;
	
	requires static lombok;
}
