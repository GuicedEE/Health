package com.guicedee.health;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to configure Health Checks for GuicedEE.
 * <p>
 * This annotation can be placed on a configuration class or a package to customize the health check
 * behavior and the HTTP endpoints where health information is exposed.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PACKAGE})
public @interface HealthOptions {
    /**
     * Whether health checks are enabled.
     * <p>
     * If set to {@code false}, health checks will not be registered and the HTTP endpoints will not be exposed.
     * Defaults to {@code true}.
     * </p>
     * @return true if enabled, false otherwise.
     */
    boolean enabled() default true;

    /**
     * The path to expose the aggregated health checks.
     * <p>
     * This endpoint provides a summary of all registered health checks (liveness, readiness, and startup).
     * Defaults to {@code /health}.
     * </p>
     * @return the health check path.
     */
    String path() default "/health";

    /**
     * The path to expose liveness checks.
     * <p>
     * Liveness checks indicate whether the application is alive and should be restarted if it fails.
     * Defaults to {@code /health/live}.
     * </p>
     * @return the liveness check path.
     */
    String livenessPath() default "/health/live";

    /**
     * The path to expose readiness checks.
     * <p>
     * Readiness checks indicate whether the application is ready to receive traffic.
     * Defaults to {@code /health/ready}.
     * </p>
     * @return the readiness check path.
     */
    String readinessPath() default "/health/ready";

    /**
     * The path to expose startup checks.
     * <p>
     * Startup checks indicate whether the application has successfully started up.
     * Defaults to {@code /health/started}.
     * </p>
     * @return the startup check path.
     */
    String startupPath() default "/health/started";
}
