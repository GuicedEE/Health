package com.guicedee.health.test;

import com.guicedee.client.IGuiceContext;
import com.guicedee.health.HealthOptions;
import com.guicedee.health.implementations.HealthPreStartup;
import io.vertx.ext.healthchecks.HealthChecks;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.health.Startup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@HealthOptions
public class HealthIntegrationTest {

    @Test
    public void testHealthCheckRegistration() throws InterruptedException, ExecutionException, TimeoutException {
        IGuiceContext.instance().inject();
        
        // Ensure HealthPreStartup is initialized if it hasn't been already
        HealthChecks hc = HealthPreStartup.getHealthChecks();
        
        // Wait a bit for postLoad to finish
        Thread.sleep(2000);
        
        HealthChecks healthChecks = IGuiceContext.get(HealthChecks.class);
        Assertions.assertNotNull(healthChecks);

        CompletableFuture<io.vertx.ext.healthchecks.CheckResult> future = new CompletableFuture<>();
        healthChecks.checkStatus()
                .onSuccess(r -> future.complete(r))
                .onFailure(e -> future.completeExceptionally(e));

        io.vertx.ext.healthchecks.CheckResult result = future.get(5, TimeUnit.SECONDS);
        Assertions.assertNotNull(result);
        System.out.println("[DEBUG_LOG] Result: " + result.toJson().encodePrettily());
        
        Assertions.assertTrue(result.getUp());
        
        // Check for our custom health checks
        boolean livenessFound = result.toJson().getJsonArray("checks").stream()
                .map(o -> (io.vertx.core.json.JsonObject) o)
                .anyMatch(j -> j.getString("id").equals(MockLivenessCheck.class.getName()));
        
        Assertions.assertTrue(livenessFound, "MockLivenessCheck not found in health checks");

        boolean readinessFound = result.toJson().getJsonArray("checks").stream()
                .map(o -> (io.vertx.core.json.JsonObject) o)
                .anyMatch(j -> j.getString("id").equals(MockReadinessCheck.class.getName()));

        Assertions.assertTrue(readinessFound, "MockReadinessCheck not found in health checks");

        boolean startupFound = result.toJson().getJsonArray("checks").stream()
                .map(o -> (io.vertx.core.json.JsonObject) o)
                .anyMatch(j -> j.getString("id").equals(MockStartupCheck.class.getName()));

        Assertions.assertTrue(startupFound, "MockStartupCheck not found in health checks");
    }

    @Test
    public void testSpecificHealthChecks() throws InterruptedException, ExecutionException, TimeoutException {
        IGuiceContext.instance().inject();
        
        // Wait a bit for postLoad to finish
        Thread.sleep(2000);

        // Liveness
        HealthChecks liveness = HealthPreStartup.getLivenessChecks();
        CompletableFuture<io.vertx.ext.healthchecks.CheckResult> lFuture = new CompletableFuture<>();
        liveness.checkStatus().onSuccess(lFuture::complete).onFailure(lFuture::completeExceptionally);
        io.vertx.ext.healthchecks.CheckResult lResult = lFuture.get(5, TimeUnit.SECONDS);
        Assertions.assertTrue(lResult.toJson().getJsonArray("checks").stream()
                .map(o -> (io.vertx.core.json.JsonObject) o)
                .anyMatch(j -> j.getString("id").equals(MockLivenessCheck.class.getName())));

        // Readiness
        HealthChecks readiness = HealthPreStartup.getReadinessChecks();
        CompletableFuture<io.vertx.ext.healthchecks.CheckResult> rFuture = new CompletableFuture<>();
        readiness.checkStatus().onSuccess(rFuture::complete).onFailure(rFuture::completeExceptionally);
        io.vertx.ext.healthchecks.CheckResult rResult = rFuture.get(5, TimeUnit.SECONDS);
        Assertions.assertTrue(rResult.toJson().getJsonArray("checks").stream()
                .map(o -> (io.vertx.core.json.JsonObject) o)
                .anyMatch(j -> j.getString("id").equals(MockReadinessCheck.class.getName())));

        // Startup
        HealthChecks startup = HealthPreStartup.getStartupChecks();
        CompletableFuture<io.vertx.ext.healthchecks.CheckResult> sFuture = new CompletableFuture<>();
        startup.checkStatus().onSuccess(sFuture::complete).onFailure(sFuture::completeExceptionally);
        io.vertx.ext.healthchecks.CheckResult sResult = sFuture.get(5, TimeUnit.SECONDS);
        Assertions.assertTrue(sResult.toJson().getJsonArray("checks").stream()
                .map(o -> (io.vertx.core.json.JsonObject) o)
                .anyMatch(j -> j.getString("id").equals(MockStartupCheck.class.getName())));
    }

    @Test
    public void testHealthOptionsConfiguration() {
        IGuiceContext.instance().inject();
        HealthOptions options = HealthPreStartup.getOptions();
        Assertions.assertNotNull(options);
        // Defaults
        Assertions.assertEquals("/health", options.path());
        Assertions.assertEquals("/health/live", options.livenessPath());
        Assertions.assertEquals("/health/ready", options.readinessPath());
        Assertions.assertEquals("/health/started", options.startupPath());
        Assertions.assertTrue(options.enabled());
    }

    @Test
    public void testDownHealthCheck() throws InterruptedException, ExecutionException, TimeoutException {
        IGuiceContext.instance().inject();
        
        HealthChecks healthChecks = IGuiceContext.get(HealthChecks.class);
        
        // Register a DOWN check manually for testing
        healthChecks.register("down-check", promise -> promise.complete(io.vertx.ext.healthchecks.Status.KO()));
        
        CompletableFuture<io.vertx.ext.healthchecks.CheckResult> future = new CompletableFuture<>();
        healthChecks.checkStatus().onSuccess(future::complete).onFailure(future::completeExceptionally);
        io.vertx.ext.healthchecks.CheckResult result = future.get(5, TimeUnit.SECONDS);
        
        Assertions.assertFalse(result.getUp());
        boolean foundDown = result.toJson().getJsonArray("checks").stream()
                .map(o -> (io.vertx.core.json.JsonObject) o)
                .anyMatch(j -> "down-check".equals(j.getString("id")) && "DOWN".equals(j.getString("status")));
        Assertions.assertTrue(foundDown);
        
        // Unregister it to clean up
        healthChecks.unregister("down-check");
    }

    @Test
    public void testHealthCheckWithData() throws InterruptedException, ExecutionException, TimeoutException {
        IGuiceContext.instance().inject();
        HealthChecks healthChecks = IGuiceContext.get(HealthChecks.class);

        CompletableFuture<io.vertx.ext.healthchecks.CheckResult> future = new CompletableFuture<>();
        healthChecks.checkStatus().onSuccess(future::complete).onFailure(future::completeExceptionally);
        io.vertx.ext.healthchecks.CheckResult result = future.get(5, TimeUnit.SECONDS);

        boolean foundWithData = result.toJson().getJsonArray("checks").stream()
                .map(o -> (io.vertx.core.json.JsonObject) o)
                .filter(j -> j.getString("id").equals(MockCheckWithData.class.getName()))
                .anyMatch(j -> j.getJsonObject("data") != null && "value".equals(j.getJsonObject("data").getString("key")));

        Assertions.assertTrue(foundWithData, "MockCheckWithData not found or missing metadata");
    }

    @Liveness
    public static class MockCheckWithData implements HealthCheck {
        @Override
        public HealthCheckResponse call() {
            return HealthCheckResponse.named("with-data")
                    .up()
                    .withData("key", "value")
                    .build();
        }
    }

    @Liveness
    public static class MockLivenessCheck implements HealthCheck {
        @Override
        public HealthCheckResponse call() {
            return HealthCheckResponse.named("mock-liveness").up().build();
        }
    }

    @Readiness
    public static class MockReadinessCheck implements HealthCheck {
        @Override
        public HealthCheckResponse call() {
            return HealthCheckResponse.named("mock-readiness").up().build();
        }
    }

    @Startup
    public static class MockStartupCheck implements HealthCheck {
        @Override
        public HealthCheckResponse call() {
            return HealthCheckResponse.named("mock-startup").up().build();
        }
    }
}
