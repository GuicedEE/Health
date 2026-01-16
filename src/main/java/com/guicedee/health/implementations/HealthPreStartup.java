package com.guicedee.health.implementations;

import com.guicedee.client.IGuiceContext;
import com.guicedee.client.services.lifecycle.IGuicePreDestroy;
import com.guicedee.client.services.lifecycle.IGuicePreStartup;
import com.guicedee.client.services.lifecycle.IGuicePostStartup;
import com.guicedee.health.HealthOptions;
import com.guicedee.vertx.spi.VertXPreStartup;
import io.vertx.core.Future;
import io.vertx.ext.healthchecks.HealthChecks;
import io.vertx.ext.healthchecks.Status;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.health.Startup;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the lifecycle and registration of health checks in GuicedEE.
 * <p>
 * This class implements multiple GuicedEE lifecycle interfaces:
 * <ul>
 *     <li>{@link IGuicePreStartup}: Initializes Vert.x {@link HealthChecks} instances and scans for {@link HealthCheck} implementations.</li>
 *     <li>{@link IGuicePostStartup}: Instantiates health checks via Guice and registers them with Vert.x health check instances.</li>
 *     <li>{@link IGuicePreDestroy}: Handles cleanup during application shutdown.</li>
 * </ul>
 * </p>
 */
public class HealthPreStartup implements IGuicePreStartup<HealthPreStartup>, IGuicePostStartup<HealthPreStartup>, IGuicePreDestroy<HealthPreStartup> {
    /**
     * The main aggregated health checks instance.
     */
    private static HealthChecks healthChecks;
    /**
     * The liveness health checks instance.
     */
    private static HealthChecks livenessChecks;
    /**
     * The readiness health checks instance.
     */
    private static HealthChecks readinessChecks;
    /**
     * The startup health checks instance.
     */
    private static HealthChecks startupChecks;

    /**
     * List of discovered health check classes.
     */
    private static final List<Class<? extends HealthCheck>> healthCheckClasses = new ArrayList<>();

    @Override
    public List<Future<Boolean>> onStartup() {
        if (healthChecks == null) {
            var vertx = VertXPreStartup.getVertx();
            healthChecks = HealthChecks.create(vertx);
            livenessChecks = HealthChecks.create(vertx);
            readinessChecks = HealthChecks.create(vertx);
            startupChecks = HealthChecks.create(vertx);

            healthCheckClasses.addAll(IGuiceContext.instance().getScanResult()
                    .getClassesImplementing(HealthCheck.class)
                    .loadClasses(HealthCheck.class));
        }
        return List.of(Future.succeededFuture(true));
    }

    @Override
    public List<Future<Boolean>> postLoad() {
        for (Class<? extends HealthCheck> clazz : healthCheckClasses) {
            HealthCheck healthCheck = IGuiceContext.get(clazz);

            boolean liveness = clazz.isAnnotationPresent(Liveness.class);
            boolean readiness = clazz.isAnnotationPresent(Readiness.class);
            boolean startup = clazz.isAnnotationPresent(Startup.class);
            boolean generic = false;
            try {
                Class<? extends java.lang.annotation.Annotation> healthClass = (Class<? extends java.lang.annotation.Annotation>) Class.forName("org.eclipse.microprofile.health.Health");
                generic = clazz.isAnnotationPresent(healthClass);
            } catch (ClassNotFoundException e) {
                // Ignore
            }

            if (liveness) {
                register(livenessChecks, healthCheck);
                if (healthChecks != livenessChecks) {
                    register(healthChecks, healthCheck);
                }
            }
            if (readiness) {
                register(readinessChecks, healthCheck);
                if (healthChecks != readinessChecks) {
                    register(healthChecks, healthCheck);
                }
            }
            if (startup) {
                register(startupChecks, healthCheck);
                if (healthChecks != startupChecks) {
                    register(healthChecks, healthCheck);
                }
            }
            if (generic || (!liveness && !readiness && !startup)) {
                register(healthChecks, healthCheck);
            }
        }
        return List.of(Future.succeededFuture(true));
    }

    /**
     * Registers a MicroProfile {@link HealthCheck} to a Vert.x {@link HealthChecks} instance.
     *
     * @param hc    The Vert.x health checks instance to register with.
     * @param check The MicroProfile health check to register.
     */
    private void register(HealthChecks hc, HealthCheck check) {
        String name = check.getClass().getName();
        hc.unregister(name);
        hc.register(name, promise -> {
            try {
                HealthCheckResponse response = check.call();
                if (response.getStatus() == HealthCheckResponse.Status.UP) {
                    promise.complete(Status.OK(new io.vertx.core.json.JsonObject(response.getData().orElse(java.util.Collections.emptyMap()))));
                } else {
                    promise.complete(Status.KO(new io.vertx.core.json.JsonObject(response.getData().orElse(java.util.Collections.emptyMap()))));
                }
            } catch (Exception e) {
                promise.fail(e);
            }
        });
    }

    /**
     * Returns the aggregated health checks instance.
     *
     * @return The HealthChecks instance.
     */
    public static HealthChecks getHealthChecks() {
        if (healthChecks == null) {
            new HealthPreStartup().onStartup();
        }
        return healthChecks;
    }

    /**
     * Returns the liveness health checks instance.
     *
     * @return The liveness HealthChecks instance.
     */
    public static HealthChecks getLivenessChecks() {
        if (livenessChecks == null) {
            new HealthPreStartup().onStartup();
        }
        return livenessChecks;
    }

    /**
     * Returns the readiness health checks instance.
     *
     * @return The readiness HealthChecks instance.
     */
    public static HealthChecks getReadinessChecks() {
        if (readinessChecks == null) {
            new HealthPreStartup().onStartup();
        }
        return readinessChecks;
    }

    /**
     * Returns the startup health checks instance.
     *
     * @return The startup HealthChecks instance.
     */
    public static HealthChecks getStartupChecks() {
        if (startupChecks == null) {
            new HealthPreStartup().onStartup();
        }
        return startupChecks;
    }

    /**
     * Retrieves the {@link HealthOptions} annotation from the application configuration.
     *
     * @return The HealthOptions annotation, or null if not found.
     */
    public static HealthOptions getOptions() {
        var healthConfig = IGuiceContext.instance().getScanResult().getClassesWithAnnotation(HealthOptions.class);
        if (healthConfig.size() == 1) {
            var clazz = healthConfig.getFirst().loadClass();
            return clazz.getDeclaredAnnotation(HealthOptions.class);
        }
        return null;
    }

    @Override
    public void onDestroy() {
        // No explicit close needed for HealthChecks
    }

    @Override
    public Integer sortOrder() {
        return Integer.MIN_VALUE + 60; // Just after VertXPreStartup (MIN_VALUE + 50)
    }
}
