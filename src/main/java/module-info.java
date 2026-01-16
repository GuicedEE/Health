/**
 * The GuicedEE Health module provides integration between Vert.x Health Checks and the MicroProfile Health API.
 * <p>
 * It automatically scans for classes implementing {@link org.eclipse.microprofile.health.HealthCheck} and
 * registers them with Vert.x {@link io.vertx.ext.healthchecks.HealthChecks} instances.
 * </p>
 * <p>
 * This module exposes several health check endpoints:
 * <ul>
 *     <li>{@code /health}: Aggregated health status</li>
 *     <li>{@code /health/live}: Liveness status (via {@link org.eclipse.microprofile.health.Liveness})</li>
 *     <li>{@code /health/ready}: Readiness status (via {@link org.eclipse.microprofile.health.Readiness})</li>
 *     <li>{@code /health/started}: Startup status (via {@link org.eclipse.microprofile.health.Startup})</li>
 * </ul>
 * </p>
 */
module com.guicedee.health {
    requires transitive com.guicedee.guicedinjection;
    requires transitive io.vertx.healthcheck;
    requires transitive io.vertx.web;
    requires transitive com.guicedee.services.health;

    requires com.guicedee.vertx.web;

    exports com.guicedee.health;

    opens com.guicedee.health to com.google.guice, com.guicedee.client, com.guicedee.guicedinjection;
    opens com.guicedee.health.implementations to com.google.guice, com.guicedee.client, com.guicedee.guicedinjection;
    exports com.guicedee.health.implementations;

    provides com.guicedee.client.services.lifecycle.IGuiceModule with com.guicedee.health.implementations.HealthModule;
    provides com.guicedee.client.services.lifecycle.IGuicePreStartup with com.guicedee.health.implementations.HealthPreStartup;
    provides com.guicedee.client.services.lifecycle.IGuicePostStartup with com.guicedee.health.implementations.HealthPreStartup;
    provides com.guicedee.vertx.web.spi.VertxRouterConfigurator with com.guicedee.health.implementations.HealthRouterConfigurator;
    provides org.eclipse.microprofile.health.spi.HealthCheckResponseProvider with com.guicedee.health.implementations.mp.GuicedHealthCheckResponseProvider;
}
