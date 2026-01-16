
open module guiced.health.test {
    requires transitive com.guicedee.health;

    requires org.junit.jupiter.api;
    requires static lombok;
    requires io.vertx.core;
    requires io.vertx.healthcheck;
    requires transitive com.guicedee.services.health;

    exports com.guicedee.health.test;
}