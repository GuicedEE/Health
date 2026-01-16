package com.guicedee.health.implementations;

import com.google.inject.AbstractModule;
import com.guicedee.client.services.lifecycle.IGuiceModule;
import io.vertx.ext.healthchecks.HealthChecks;

/**
 * Guice module for the Health component.
 * <p>
 * This module is responsible for binding the Vert.x {@link HealthChecks} instance into the Guice context,
 * making it available for injection into other services or verticles.
 * </p>
 */
public class HealthModule extends AbstractModule implements IGuiceModule<HealthModule> {
    @Override
    protected void configure() {
        HealthChecks healthChecks = HealthPreStartup.getHealthChecks();
        bind(HealthChecks.class).toInstance(healthChecks);
    }
}
