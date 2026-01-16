package com.guicedee.health.implementations;

import com.guicedee.health.HealthOptions;
import com.guicedee.vertx.web.spi.VertxRouterConfigurator;
import io.vertx.ext.healthchecks.HealthChecks;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.healthchecks.HealthCheckHandler;

/**
 * Configures the Vert.x Web Router to expose health check endpoints.
 * <p>
 * This configurator reads {@link HealthOptions} to determine the paths for the following endpoints:
 * <ul>
 *     <li>Aggregated Health: defaults to {@code /health}</li>
 *     <li>Liveness: defaults to {@code /health/live}</li>
 *     <li>Readiness: defaults to {@code /health/ready}</li>
 *     <li>Startup: defaults to {@code /health/started}</li>
 * </ul>
 * Each endpoint is backed by a Vert.x {@link HealthCheckHandler} initialized with the corresponding
 * {@link HealthChecks} instance from {@link HealthPreStartup}.
 * </p>
 */
public class HealthRouterConfigurator implements VertxRouterConfigurator {
    @Override
    public Router builder(Router router) {
        HealthOptions options = HealthPreStartup.getOptions();
        if (options != null && !options.enabled()) {
            return router;
        }

        HealthChecks healthChecks = HealthPreStartup.getHealthChecks();
        HealthChecks livenessChecks = HealthPreStartup.getLivenessChecks();
        HealthChecks readinessChecks = HealthPreStartup.getReadinessChecks();
        HealthChecks startupChecks = HealthPreStartup.getStartupChecks();

        String healthPath = options != null ? options.path() : "/health";
        String livenessPath = options != null ? options.livenessPath() : "/health/live";
        String readinessPath = options != null ? options.readinessPath() : "/health/ready";
        String startupPath = options != null ? options.startupPath() : "/health/started";

        router.get(healthPath).handler(HealthCheckHandler.createWithHealthChecks(healthChecks));
        router.get(livenessPath).handler(HealthCheckHandler.createWithHealthChecks(livenessChecks));
        router.get(readinessPath).handler(HealthCheckHandler.createWithHealthChecks(readinessChecks));
        router.get(startupPath).handler(HealthCheckHandler.createWithHealthChecks(startupChecks));

        return router;
    }
}
