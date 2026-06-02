package com.guicedee.health.implementations;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Aggregates health status from all services in the service registry.
 * Only activates when service-registry is on the classpath.
 * <p>
 * Enable via environment variable: HEALTH_DOWNSTREAM_ENABLED=true
 */
@Readiness
public class DownstreamHealthCheck implements HealthCheck
{
    private static final String ENV_ENABLED = "HEALTH_DOWNSTREAM_ENABLED";
    private static final String SERVICE_REGISTRY_CLASS = "com.guicedee.service.registry.ServiceRegistry";
    private static final String SERVICE_ENTRY_CLASS = "com.guicedee.service.registry.ServiceEntry";

    @Override
    public HealthCheckResponse call()
    {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("downstream-services");

        // Check if enabled
        String enabled = System.getenv(ENV_ENABLED);
        if (enabled == null)
        {
            enabled = System.getProperty(ENV_ENABLED, "true");
        }
        if (!"true".equalsIgnoreCase(enabled))
        {
            return builder.up().withData("status", "disabled").build();
        }

        // Check if service-registry is available
        try
        {
            Class<?> registryClass = Class.forName(SERVICE_REGISTRY_CLASS);
            Method allMethod = registryClass.getMethod("all");

            @SuppressWarnings("unchecked")
            Map<String, Object> services = (Map<String, Object>) allMethod.invoke(null);

            if (services == null || services.isEmpty())
            {
                return builder.up().withData("services", "none registered").build();
            }

            boolean allHealthy = true;
            int upCount = 0;
            int downCount = 0;

            for (Map.Entry<String, Object> entry : services.entrySet())
            {
                String serviceName = entry.getKey();
                Object serviceEntry = entry.getValue();

                // Call isHealthy() via reflection
                Method isHealthyMethod = serviceEntry.getClass().getMethod("isHealthy");
                boolean healthy = (boolean) isHealthyMethod.invoke(serviceEntry);

                // Call status() via reflection
                Method statusMethod = serviceEntry.getClass().getMethod("status");
                Object status = statusMethod.invoke(serviceEntry);

                builder.withData(serviceName, status != null ? status.toString() : "UNKNOWN");

                if (healthy)
                {
                    upCount++;
                }
                else
                {
                    downCount++;
                    allHealthy = false;
                }
            }

            builder.withData("total", String.valueOf(services.size()));
            builder.withData("up", String.valueOf(upCount));
            builder.withData("down", String.valueOf(downCount));

            if (allHealthy)
            {
                builder.up();
            }
            else
            {
                builder.down();
            }
        }
        catch (ClassNotFoundException e)
        {
            // Service registry not on classpath — skip silently
            return builder.up().withData("status", "service-registry not available").build();
        }
        catch (Exception e)
        {
            return builder.down().withData("error", e.getMessage()).build();
        }

        return builder.build();
    }
}

