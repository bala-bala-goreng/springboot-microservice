# Config Server

Spring Cloud Config Server provides centralized configuration management for all microservices.

## Features

- Centralized configuration storage
- Native file system backend (no Git required)
- Service-specific and shared configurations
- Profile-based configuration (default, docker, development)
- Configuration refresh via actuator endpoint

## Port

- **8888** - Config Server API

## Configuration Repository

Configuration files are stored in `config-server/src/main/resources/config-repo/`:

- `application.yml` - Shared configuration for all services
- `service-gateway.yml` - Gateway service configuration
- `service-authentication.yml` - Authentication service configuration
- `service-account.yml` - Account service configuration
- `service-payment.yml` - Payment service configuration
- `service-eureka.yml` - Eureka service configuration

## Accessing Configuration

### Via HTTP API

```bash
# Get configuration for a service
curl http://localhost:8888/service-gateway/default

# Get configuration for specific profile
curl http://localhost:8888/service-gateway/docker
```

### Configuration Format

URL pattern: `/{application}/{profile}`

- `application`: Service name (e.g., `service-gateway`)
- `profile`: Environment profile (e.g., `default`, `docker`, `development`)

## Refresh Configuration

Services can refresh their configuration without restart:

```bash
# Refresh configuration (requires @RefreshScope on beans)
curl -X POST http://localhost:8080/actuator/refresh
```

## Service Integration

Services connect to config server via:

```yaml
spring:
  config:
    import: optional:configserver:http://config-server:8888
```
