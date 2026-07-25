import com.guicedee.client.services.lifecycle.IGuiceModule;

open module activity.master.image.tests {
    requires transitive com.entityassist;
    requires transitive com.guicedee.persistence;

    requires org.junit.jupiter.api;

    requires jakarta.xml.bind;
    requires jakarta.persistence;
    requires jakarta.validation;

    requires transitive org.hibernate.reactive;
    requires io.smallrye.mutiny;
    requires com.google.guice;
    requires static lombok;

    requires java.desktop;

    requires org.testcontainers;
    requires io.vertx.sql.client.pg;

    requires com.guicedee.activitymaster.fsdm;
    requires com.guicedee.activitymaster.fsdm.client;
    requires com.guicedee.activitymaster.imagemaster;

    provides IGuiceModule with com.guicedee.activitymaster.imagemaster.test.PostgreSQLTestDBModule;
}

