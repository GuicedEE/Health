# GuicedEE Health

[![Build](https://github.com/GuicedEE/Health/actions/workflows/build.yml/badge.svg)](https://github.com/GuicedEE/GuicedHealth/actions/workflows/build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.guicedee/health)](https://central.sonatype.com/artifact/com.guicedee/health)
[![Snapshot](https://img.shields.io/badge/Snapshot-2.0.1-SNAPSHOT-SNAPSHOT-orange)](https://github.com/GuicedEE/Packages/packages/maven/com.guicedee.health)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue)](https://www.apache.org/licenses/LICENSE-2.0)

![Java 25+](https://img.shields.io/badge/Java-25%2B-green)
![Guice 7](https://img.shields.io/badge/Guice-7%2B-green)
![Vert.X 5](https://img.shields.io/badge/Vert.x-5%2B-green)
![Maven 4](https://img.shields.io/badge/Maven-4%2B-green)

Seamless **MicroProfile Health** integration for [GuicedEE](https://github.com/GuicedEE) applications using **Vert.x 5 Health Checks**.
Annotate your classes with standard `@Liveness`, `@Readiness`, and `@Startup` — health checks are discovered at startup via ClassGraph, registered with Vert.x `HealthChecks`, and exposed as JSON endpoints on the Vert.x Web `Router` automatically.

Built on [Vert.x Health Checks](https://vertx.io/docs/vertx-health-check/java/) · [MicroProfile Health](https://github.com/eclipse/microprofile-health) · [Google Guice](https://github.com/google/guice) · JPMS module `com.guicedee.health` · Java 25+

## 📦 Installation

```xml
<dependency>
  <groupId>com.guicedee</groupId>
  <artifactId>health</artifactId>
</dependency>
```

<details>
<summary>Gradle (Kotlin DSL)</summary>

```kotlin
implementation("com.guicedee:health:2.0.1-SNAPSHOT")
```
</details>

## ✨ Features

- **MicroProfile Health annotations** — `@Liveness`, `@Readiness`, `@Startup`, and the legacy `@Health` are all supported
- **Automatic discovery** — `HealthPreStartup` scans for `HealthCheck` implementations via ClassGraph and registers them with the appropriate Vert.x `HealthChecks` instance
- **Four dedicated endpoints** — aggregated `/health`, plus `/health/live`, `/health/ready`, and `/health/started`
- **Configurable paths** — use `@HealthOptions` on any class (or package) to override default endpoint paths
- **Environment variable overrides** — `HEALTH_ENABLED`, `HEALTH_PATH`, `HEALTH_LIVENESS_PATH`, `HEALTH_READINESS_PATH`, `HEALTH_STARTUP_PATH` override annotation values
- **Guice-managed checks** — health check instances are obtained from the Guice injector, so `@Inject` works inside them
- **Manual registration** — inject the `HealthChecks` instance and register Vert.x-native checks directly
- **MicroProfile SPI** — custom `HealthCheckResponseProvider` and `HealthCheckResponseBuilder` provided out of the box
- **Lifecycle-aware** — integrated with `IGuicePreStartup` (scan), `IGuicePostStartup` (register), and `IGuicePreDestroy` (cleanup)
- **Timeout protection** — each registered check has a 2-second timeout to prevent hanging health endpoints

## 🚀 Quick Start

**Step 1** — Implement a health check with a MicroProfile annotation:

```java
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

@Liveness
public class DatabaseLiveness implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("DatabaseLiveness")
                .up()
                .withData("connection", "stable")
                .build();
    }
}
```

**Step 2** — Bootstrap GuicedEE (health endpoints are registered automatically):

```java
IGuiceContext.registerModuleForScanning.add("my.app");
IGuiceContext.instance();
// GET /health        → aggregated status
// GET /health/live   → liveness checks only
// GET /health/ready  → readiness checks only
// GET /health/started → startup checks only
```

No JPMS `provides` declaration is needed for health check classes — they are discovered via classpath scanning. The health module itself is registered automatically.

## 📐 Startup Flow

```mermaid
flowchart TD
    n1["IGuiceContext.instance()"]
    n2["IGuicePreStartup hooks"]
    n1 --> n2
    n3["HealthPreStartup.onStartup()<br/>sortOrder = MIN_VALUE + 60"]
    n2 --> n3
    n4["Create 4 HealthChecks instances<br/>health, liveness, readiness, startup"]
    n3 --> n4
    n5["Scan for HealthCheck implementations via ClassGraph"]
    n3 --> n5
    n6["Guice injector created"]
    n1 --> n6
    n7["HealthModule.configure()"]
    n6 --> n7
    n8["bind(HealthChecks.class).toInstance(healthChecks)"]
    n7 --> n8
    n9["IGuicePostStartup hooks"]
    n1 --> n9
    n10["HealthPreStartup.postLoad()"]
    n9 --> n10
    n11["Instantiate each discovered HealthCheck via Guice"]
    n10 --> n11
    n12["Inspect @Liveness, @Readiness, @Startup annotations"]
    n10 --> n12
    n13["Register with corresponding HealthChecks instance(s)"]
    n10 --> n13
    n14["Un-annotated checks register with the aggregated instance only"]
    n10 --> n14
    n15["VertxWebServerPostStartup<br/>from web module"]
    n9 --> n15
    n16["HealthRouterConfigurator.builder()<br/>sortOrder = MIN_VALUE + 60"]
    n15 --> n16
    n17["Read @HealthOptions / env vars for paths"]
    n16 --> n17
    n18["Mount HealthCheckHandler on each endpoint"]
    n16 --> n18
```

## 🏥 Health Check Types

### Liveness

Liveness checks determine if the application is running correctly. A failing liveness check typically means the application should be restarted.

```java
@Liveness
public class ProcessLiveness implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("ProcessLiveness")
                .up()
                .build();
    }
}
```

### Readiness

Readiness checks determine if the application is ready to receive traffic. A failing readiness check means the application should be temporarily removed from the load balancer.

```java
@Readiness
public class ServiceReadiness implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        boolean ready = checkExternalDependencies();
        return HealthCheckResponse.named("ServiceReadiness")
                .status(ready)
                .build();
    }
}
```

### Startup

Startup checks verify if the application has finished its initialization. Failing startup checks prevent liveness probes from running during long startup sequences.

```java
@Startup
public class InitializerStartup implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("InitializerStartup")
                .up()
                .build();
    }
}
```

### Un-annotated (Generic)

A `HealthCheck` implementation without any annotation is registered with the aggregated `/health` endpoint only.

```java
public class GenericCheck implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("GenericCheck")
                .up()
                .withData("version", "1.0")
                .build();
    }
}
```

### Multiple annotations

A check can carry more than one annotation — it will be registered with each corresponding instance **and** the aggregated instance:

```java
@Liveness
@Readiness
public class CriticalServiceCheck implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("CriticalService")
                .up()
                .build();
    }
}
```

## ⚙️ Configuration

### `@HealthOptions` annotation

Place `@HealthOptions` on any class or package to customize endpoints:

```java
@HealthOptions(
    enabled = true,
    path = "/health",
    livenessPath = "/health/live",
    readinessPath = "/health/ready",
    startupPath = "/health/started"
)
public class MyAppConfig {
}
```

| Attribute | Default | Description |
|---|---|---|
| `enabled` | `true` | Enables or disables health check endpoints |
| `path` | `/health` | Aggregated health status endpoint |
| `livenessPath` | `/health/live` | Liveness checks endpoint |
| `readinessPath` | `/health/ready` | Readiness checks endpoint |
| `startupPath` | `/health/started` | Startup checks endpoint |

### Environment variable overrides

Every `@HealthOptions` attribute can be overridden with a system property or environment variable:

| Variable | Overrides | Example |
|---|---|---|
| `HEALTH_ENABLED` | `enabled` | `false` |
| `HEALTH_PATH` | `path` | `/api/health` |
| `HEALTH_LIVENESS_PATH` | `livenessPath` | `/api/health/live` |
| `HEALTH_READINESS_PATH` | `readinessPath` | `/api/health/ready` |
| `HEALTH_STARTUP_PATH` | `startupPath` | `/api/health/started` |

Environment variables take precedence over annotation values.

## 🛠 Manual Registration

If you need to register health checks manually using the Vert.x API, inject the `HealthChecks` instance:

### Constructor injection

```java
import io.vertx.ext.healthchecks.HealthChecks;
import io.vertx.ext.healthchecks.Status;
import com.google.inject.Inject;

public class MyService {
    @Inject
    public MyService(HealthChecks hc) {
        hc.register("my-check", 2000, promise -> promise.complete(Status.OK()));
    }
}
```

### Field injection

```java
import io.vertx.ext.healthchecks.HealthChecks;
import io.vertx.ext.healthchecks.Status;
import com.google.inject.Inject;

public class MyOtherService {
    @Inject
    private HealthChecks hc;

    public void registerChecks() {
        hc.register("my-field-check", 2000, promise ->
            promise.complete(Status.OK()));
    }
}
```

### Accessing specific instances

The aggregated `HealthChecks` is what Guice binds. For direct access to the liveness, readiness, or startup instances, use the static accessors:

```java
HealthChecks liveness = HealthPreStartup.getLivenessChecks();
HealthChecks readiness = HealthPreStartup.getReadinessChecks();
HealthChecks startup = HealthPreStartup.getStartupChecks();
```

## 📋 Response Format

Health endpoints return standard Vert.x health check JSON:

```json
{
  "status": "UP",
  "checks": [
    {
      "id": "com.example.DatabaseLiveness",
      "status": "UP",
      "data": {
        "connection": "stable"
      }
    },
    {
      "id": "com.example.ServiceReadiness",
      "status": "UP"
    }
  ]
}
```

| HTTP Status | Meaning |
|---|---|
| `200 OK` | All checks are UP |
| `503 Service Unavailable` | One or more checks are DOWN |

### Default checks

When no `HealthCheck` implementations are found on the classpath, four placeholder checks are registered automatically (`guicedee-health`, `guicedee-liveness`, `guicedee-readiness`, `guicedee-startup`) — all returning `Status.OK` — so the endpoints always respond.

## 🗺️ Module Graph

```mermaid
flowchart LR
    com_guicedee_health["com.guicedee.health"]
    com_guicedee_health --> com_guicedee_guicedinjection["com.guicedee.guicedinjection<br/>GuicedEE runtime — scanning, Guice, lifecycle"]
    com_guicedee_health --> com_guicedee_vertx_web["com.guicedee.vertx.web<br/>Vert.x Web — Router, route mounting"]
    com_guicedee_health --> io_vertx_healthcheck["io.vertx.healthcheck<br/>Vert.x Health Checks — HealthChecks, HealthCheckHandler"]
    com_guicedee_health --> io_vertx_web["io.vertx.web<br/>Vert.x Web — Router integration"]
    com_guicedee_health --> io_vertx_core["io.vertx.core<br/>Vert.x core"]
    com_guicedee_health --> com_guicedee_modules_services_health["com.guicedee.modules.services.health<br/>MicroProfile Health API — annotations, SPI"]
```

## 🧩 JPMS

Module name: **`com.guicedee.health`**

The module:
- **exports** `com.guicedee.health` and `com.guicedee.health.implementations`
- **provides** `IGuiceModule` with `HealthModule`
- **provides** `IGuicePreStartup` with `HealthPreStartup`
- **provides** `IGuicePostStartup` with `HealthPreStartup`
- **provides** `VertxRouterConfigurator` with `HealthRouterConfigurator`
- **provides** `HealthCheckResponseProvider` with `GuicedHealthCheckResponseProvider`

## 🏗️ Key Classes

| Class | Role |
|---|---|
| `HealthOptions` | Annotation — configures endpoint paths and enable/disable |
| `HealthPreStartup` | `IGuicePreStartup` + `IGuicePostStartup` + `IGuicePreDestroy` — scans, registers, and manages health check lifecycle |
| `HealthModule` | `IGuiceModule` — binds the `HealthChecks` instance into Guice |
| `HealthRouterConfigurator` | `VertxRouterConfigurator` — mounts `HealthCheckHandler` on the Vert.x Router |
| `GuicedHealthCheckResponseProvider` | MicroProfile `HealthCheckResponseProvider` SPI — creates response builders |

## 🤝 Contributing

Issues and pull requests are welcome — please add tests for new health check integrations.

## 📄 License

[Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0)
