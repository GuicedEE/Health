package com.guicedee.health.test;

import com.guicedee.client.IGuiceContext;
import com.guicedee.health.implementations.DownstreamHealthCheck;
import com.guicedee.health.implementations.HealthPreStartup;
import com.guicedee.service.registry.ServiceEntry;
import com.guicedee.service.registry.ServiceRegistry;
import com.guicedee.service.registry.ServiceStatus;
import io.vertx.ext.healthchecks.CheckResult;
import io.vertx.ext.healthchecks.HealthChecks;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class DownstreamHealthTest
{
    @BeforeEach
    void setup()
    {
        ServiceRegistry.clear();
    }

    @Test
    public void testDownstreamAllHealthy()
    {
        IGuiceContext.instance().inject();

        // Register mock services
        ServiceRegistry.register(new ServiceEntry("service-a", "http://service-a:8080", "/health/ready",
                ServiceStatus.UP, Instant.now(), Map.of()));
        ServiceRegistry.register(new ServiceEntry("service-b", "http://service-b:8080", "/health/ready",
                ServiceStatus.UP, Instant.now(), Map.of()));

        DownstreamHealthCheck check = new DownstreamHealthCheck();
        HealthCheckResponse response = check.call();

        Assertions.assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
        Assertions.assertTrue(response.getData().isPresent());
        Assertions.assertEquals("2", response.getData().get().get("total").toString());
        Assertions.assertEquals("2", response.getData().get().get("up").toString());
        Assertions.assertEquals("0", response.getData().get().get("down").toString());
    }

    @Test
    public void testDownstreamSomeUnhealthy()
    {
        IGuiceContext.instance().inject();

        ServiceRegistry.register(new ServiceEntry("service-a", "http://service-a:8080", "/health/ready",
                ServiceStatus.UP, Instant.now(), Map.of()));
        ServiceRegistry.register(new ServiceEntry("service-b", "http://service-b:8080", "/health/ready",
                ServiceStatus.DOWN, Instant.now(), Map.of()));

        DownstreamHealthCheck check = new DownstreamHealthCheck();
        HealthCheckResponse response = check.call();

        Assertions.assertEquals(HealthCheckResponse.Status.DOWN, response.getStatus());
        Assertions.assertEquals("1", response.getData().get().get("up").toString());
        Assertions.assertEquals("1", response.getData().get().get("down").toString());
    }

    @Test
    public void testDownstreamNoServices()
    {
        IGuiceContext.instance().inject();

        DownstreamHealthCheck check = new DownstreamHealthCheck();
        HealthCheckResponse response = check.call();

        Assertions.assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
        Assertions.assertTrue(response.getData().isPresent());
        Assertions.assertEquals("none registered", response.getData().get().get("services").toString());
    }

    @Test
    public void testDownstreamDisabled()
    {
        IGuiceContext.instance().inject();

        System.setProperty("HEALTH_DOWNSTREAM_ENABLED", "false");
        try
        {
            DownstreamHealthCheck check = new DownstreamHealthCheck();
            HealthCheckResponse response = check.call();

            Assertions.assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
            Assertions.assertEquals("disabled", response.getData().get().get("status").toString());
        }
        finally
        {
            System.clearProperty("HEALTH_DOWNSTREAM_ENABLED");
        }
    }

    @Test
    public void testDownstreamDegradedIsHealthy()
    {
        IGuiceContext.instance().inject();

        ServiceRegistry.register(new ServiceEntry("service-a", "http://service-a:8080", "/health/ready",
                ServiceStatus.DEGRADED, Instant.now(), Map.of()));

        DownstreamHealthCheck check = new DownstreamHealthCheck();
        HealthCheckResponse response = check.call();

        Assertions.assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
        Assertions.assertEquals("1", response.getData().get().get("up").toString());
    }

    @Test
    public void testDownstreamIntegrationWithHealthChecks() throws Exception
    {
        IGuiceContext.instance().inject();
        Thread.sleep(2000);

        ServiceRegistry.register(new ServiceEntry("integration-svc", "http://integration:8080", "/health/ready",
                ServiceStatus.UP, Instant.now(), Map.of()));

        HealthChecks readiness = HealthPreStartup.getReadinessChecks();
        CompletableFuture<CheckResult> future = new CompletableFuture<>();
        readiness.checkStatus().onSuccess(future::complete).onFailure(future::completeExceptionally);
        CheckResult result = future.get(5, TimeUnit.SECONDS);

        // The DownstreamHealthCheck should be registered as a readiness check
        boolean found = result.toJson().getJsonArray("checks").stream()
                .map(o -> (io.vertx.core.json.JsonObject) o)
                .anyMatch(j -> j.getString("id").contains("DownstreamHealthCheck"));

        Assertions.assertTrue(found, "DownstreamHealthCheck should be registered as readiness check");
    }
}



