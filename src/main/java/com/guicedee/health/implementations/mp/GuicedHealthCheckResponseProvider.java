package com.guicedee.health.implementations.mp;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.spi.HealthCheckResponseProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Custom implementation of {@link HealthCheckResponseProvider} for GuicedEE.
 * <p>
 * This provider is used by the MicroProfile Health API to create {@link HealthCheckResponseBuilder} instances.
 * It is registered as a service provider via {@code module-info.java} and {@code META-INF/services}.
 * </p>
 */
public class GuicedHealthCheckResponseProvider implements HealthCheckResponseProvider {
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
        private String name;
        private HealthCheckResponse.Status status = HealthCheckResponse.Status.DOWN;
        private Map<String, Object> data = new HashMap<>();

        @Override
        public HealthCheckResponseBuilder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public HealthCheckResponseBuilder up() {
            this.status = HealthCheckResponse.Status.UP;
            return this;
        }

        @Override
        public HealthCheckResponseBuilder down() {
            this.status = HealthCheckResponse.Status.DOWN;
            return this;
        }

        @Override
        public HealthCheckResponseBuilder status(boolean up) {
            if (up) {
                return up();
            }
            return down();
        }

        @Override
        public HealthCheckResponseBuilder withData(String key, String value) {
            data.put(key, value);
            return this;
        }

        @Override
        public HealthCheckResponseBuilder withData(String key, long value) {
            data.put(key, value);
            return this;
        }

        @Override
        public HealthCheckResponseBuilder withData(String key, boolean value) {
            data.put(key, value);
            return this;
        }

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
        private final String name;
        private final Status status;
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

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Status getStatus() {
            return status;
        }

        @Override
        public Optional<Map<String, Object>> getData() {
            return Optional.ofNullable(data);
        }
    }
}
