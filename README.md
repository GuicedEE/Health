# 🏥 GuicedEE Health

[![JDK](https://img.shields.io/badge/JDK-25%2B-0A7?logo=java)](https://openjdk.org/projects/jdk/25/)
[![Build](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

The GuicedEE Health module provides a seamless integration between **Vert.x 5 Health Checks** and the **Eclipse MicroProfile Health API**. It allows developers to define health checks using standard MicroProfile annotations and exposes them through Vert.x Web endpoints.

## ✨ Features

- **MicroProfile Health Support**: Full support for `@Liveness`, `@Readiness`, and `@Startup` annotations.
- **Automatic Discovery**: Automatically scans and registers health check classes in the Guice context.
- **Vert.x 5 Integration**: Leverages `vertx-health-check` for robust, non-blocking health monitoring.
- **Configurable Endpoints**: Custom paths for health, liveness, readiness, and startup checks via annotations.
- **Aggregated Status**: A unified `/health` endpoint that combines all registered checks.
- **Lifecycle Aware**: Integrated with GuicedEE's `PreStartup` and `PostStartup` phases for safe initialization.

## 📦 Install (Maven)

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.guicedee</groupId>
    <artifactId>guiced-health</artifactId>
</dependency>
```

## 🚀 Quick Start

### 1. Configure Health Endpoints

Use the `@HealthOptions` annotation on your application class or package to customize the health check behavior.

```java
@HealthOptions(
    enabled = true,
    path = "/health",
    livenessPath = "/health/live",
    readinessPath = "/health/ready",
    startupPath = "/health/started"
)
public class MyConfiguration {
}
```

#### Configuration Options

| Attribute | Default Value | Description |
| :--- | :--- | :--- |
| `enabled` | `true` | Enables or disables the health check module. |
| `path` | `/health` | Path for the aggregated health status. |
| `livenessPath` | `/health/live` | Path for liveness checks. |
| `readinessPath` | `/health/ready` | Path for readiness checks. |
| `startupPath` | `/health/started` | Path for startup checks. |

### 2. Implement Health Checks

Implement the `org.eclipse.microprofile.health.HealthCheck` interface and annotate it with the appropriate scope.

#### Liveness Check
Liveness checks determine if the application is running correctly.

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

#### Readiness Check
Readiness checks determine if the application is ready to process requests.

```java
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
public class ServiceReadiness implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("ServiceReadiness")
                .up()
                .build();
    }
}
```

#### Startup Check
Startup checks verify if the application has finished its initialization.

```java
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Startup;

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

## 🛠 Manual Registration

If you need to register health checks manually using the Vert.x API, you can inject the `HealthChecks` instance:

```java
import io.vertx.ext.healthchecks.HealthChecks;
import io.vertx.ext.healthchecks.Status;
import com.google.inject.Inject;

public class MyService {
    @Inject
    public MyService(HealthChecks hc) {
        hc.register("my-check", promise -> promise.complete(Status.OK()));
    }
}
```

## 📚 Docs & Rules
- Rules: `RULES.md`
- Guides: `GUIDES.md`

## 🤝 Contributing
Contributions are welcome! Please follow the existing code style and ensure all tests pass before submitting a PR.

## 📝 License
This project is licensed under the terms of the Apache License, Version 2.0.
