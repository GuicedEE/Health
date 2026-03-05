package com.guicedee.health.implementations.mp;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.spi.HealthCheckResponseProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Custom implementation of {@link HealthCheckResponseProvider} for GuicedEE.
 *
 * <p>This provider is used by the MicroProfile Health API to create {@link HealthCheckResponseBuilder} instances.
 * It is registered as a service provider via {@code module-info.java} and {@code META-INF/services}.
 */
public class GuicedHealthCheckResponseProvider implements HealthCheckResponseProvider {

    /**
     * Creates a new {@link GuicedHealthCheckResponseProvider}.
     */
    public GuicedHealthCheckResponseProvider() {
    }

    /**
     * Creates a new instance of {@link GuicedHealthCheckResponseBuilder}.
     *
     * @return A new response builder.
     */
    @Override
    public HealthCheckResponseBuilder createResponseBuilder() {
        return new GuicedHealthCheckResponseBuilder();
    }

    /**
     * Custom implementation of {@link HealthCheckResponseBuilder} for GuicedEE.
     */
    public static class GuicedHealthCheckResponseBuilder extends HealthCheckResponseBuilder {
        /**
         * The name of the health check.
         */
        private String name;
        /**
         * The status of the health check, defaults to {@link HealthCheckResponse.Status#DOWN}.
         */
        private HealthCheckResponse.Status status = HealthCheckResponse.Status.DOWN;
        /**
         * Optional metadata associated with the health check.
         */
        private Map<String, Object> data = new HashMap<>();

        /**
         * Creates a new {@link GuicedHealthCheckResponseBuilder}.
         */
        public GuicedHealthCheckResponseBuilder() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public HealthCheckResponseBuilder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public HealthCheckResponseBuilder up() {
            this.status = HealthCheckResponse.Status.UP;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public HealthCheckResponseBuilder down() {
            this.status = HealthCheckResponse.Status.DOWN;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public HealthCheckResponseBuilder status(boolean up) {
            if (up) {
                return up();
            }
            return down();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public HealthCheckResponseBuilder withData(String key, String value) {
            data.put(key, value);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public HealthCheckResponseBuilder withData(String key, long value) {
            data.put(key, value);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public HealthCheckResponseBuilder withData(String key, boolean value) {
            data.put(key, value);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public HealthCheckResponse build() {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Health check name must not be null or empty");
            }
            return new GuicedHealthCheckResponse(name, status, data.isEmpty() ? null : data);
        }
    }

    /**
     * Custom implementation of {@link HealthCheckResponse} for GuicedEE.
     */
    public static class GuicedHealthCheckResponse extends HealthCheckResponse {
        /**
         * The name of the health check.
         */
        private final String name;
        /**
         * The status of the health check.
         */
        private final Status status;
        /**
         * Optional metadata associated with the health check.
         */
        private final Map<String, Object> data;

        /**
         * Constructs a new health check response.
         *
         * @param name   The name of the health check.
         * @param status The status of the health check.
         * @param data   Optional metadata associated with the health check.
         */
        public GuicedHealthCheckResponse(String name, Status status, Map<String, Object> data) {
            this.name = name;
            this.status = status;
            this.data = data;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return name;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Status getStatus() {
            return status;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<Map<String, Object>> getData() {
            return Optional.ofNullable(data);
        }
    }
}
