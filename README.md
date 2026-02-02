# Spring Boot Microservices Boilerplate

A ready-to-use microservices setup with Spring Boot 3.x. Everything you need to build and run distributed services: service discovery, API gateway, authentication, monitoring, and logging.

## What's Inside

This boilerplate gives you:

- **Service Discovery** - Services find each other automatically using Eureka
- **API Gateway** - Single entry point that routes requests and handles auth
- **Authentication** - OAuth2 token generation and validation for partners
- **Shared Library** - Common code used across all services
- **Monitoring** - Prometheus for metrics, Grafana for dashboards, Tempo for tracing
- **Logging** - Centralized logs with Loki, queryable in Grafana
- **Docker Setup** - Everything runs in containers with docker-compose

## How It Works

```
Client → Gateway (8999) → Services (8080, 8081, 8082)
                              ↓
                    Eureka (8761) + PostgreSQL (5432)
                              ↓
                    Monitoring (Prometheus, Grafana, Tempo, Loki)
```

**Services:**
- **Gateway** (8999) - Routes requests, validates tokens
- **Authentication** (8080) - OAuth2 tokens, partner management
- **Account** (8081) - User and account management
- **Payment** (8082) - Payment processing
- **Eureka** (8761) - Service registry
- **Config Server** (8888) - Centralized configuration

## Project Structure

```
springboot-microservice/
├── core/              # Shared library (logging, tracing, utils)
├── eureka/            # Service discovery server
├── config-server/      # Configuration management
├── gateway/            # API gateway
├── authentication/     # Auth service
├── account/           # Account service
├── payment/           # Payment service
└── docker/            # Docker configs and compose file
```

## Quick Start

### Using Docker (Easiest)

```bash
cd docker
docker-compose up -d
```

That's it. All services start automatically.

**Access Points:**
- Gateway: http://localhost:8999
- Eureka: http://localhost:8761
- Config Server: http://localhost:8888
- Grafana: http://localhost:3000 (admin/admin)
- Prometheus: http://localhost:9090
- Loki: http://localhost:3100

### Running Locally

1. Build the core library first:
   ```bash
   cd core
   mvn clean install
   ```

2. Start PostgreSQL and Eureka:
   ```bash
   cd docker
   docker-compose up -d postgresql eureka
   ```

3. Run each service:
   ```bash
   # In separate terminals
   cd authentication && mvn spring-boot:run
   cd account && mvn spring-boot:run
   cd payment && mvn spring-boot:run
   cd gateway && mvn spring-boot:run
   ```

## How Requests Flow

1. Client sends request to Gateway (8999) with JWT token
2. Gateway validates token with Authentication service
3. Gateway routes request to the right service (Account, Payment, etc.)
4. Service processes request, talks to database if needed
5. Response goes back through Gateway to client

**Tracing:** Every request gets a trace ID. You can see the full journey from gateway → service → database in Grafana Tempo.

## Authentication

### Get a Token

```bash
curl -X POST "http://localhost:8080/api/oauth/token" \
  -H "Authorization: Basic $(echo -n 'merchant-x-api-key-456:merchant-x-secret-key-123' | base64)" \
  -H "Content-Type: application/json" \
  -d '{"grant_type":"client_credentials"}'
```

### Use the Token

```bash
curl -X GET "http://localhost:8999/api/accounts" \
  -H "Authorization: Bearer <your-token>"
```

## Monitoring

### Grafana
- **URL**: http://localhost:3000
- **Login**: admin/admin
- **What you get**: Pre-built dashboards for services, metrics, and traces

### Prometheus
- **URL**: http://localhost:9090
- **What it does**: Collects metrics from all services

### Tempo (Tracing)
- **Access**: Grafana → Explore → Select Tempo
- **What you see**: Full request traces from gateway to database

### Loki (Logs)
- **Access**: Grafana → Explore → Select Loki
- **What you get**: All service logs in one place

**Query Examples:**
```logql
# All request logs
{container_name=~".*"} | json | operation_name="REQUEST"

# Error logs
{container_name=~".*"} | json | level="ERROR"

# Logs for specific trace
{container_name=~".*"} | json | trace_id="abc123..."
```

## Configuration

All service configs live in `config-server/src/main/resources/config-repo/`. Each service has its own file:
- `service-gateway.yml`
- `service-authentication.yml`
- `service-account.yml`
- `service-payment.yml`

Services automatically pull configs from the config server on startup.

## Database

PostgreSQL is used by all services. Database is initialized automatically with sample data when you start the stack.

**Connection:**
- Host: localhost
- Port: 5432
- Database: app_db
- User: app_user
- Password: app_password

## Tech Stack

- **Java 21**
- **Spring Boot 3.5.10**
- **Spring Cloud 2025.0.0**
- **PostgreSQL**
- **Eureka** (service discovery)
- **Prometheus + Grafana** (monitoring)
- **Tempo** (tracing)
- **Loki + Promtail** (logging)

## Adding a New Service

1. Create a new module (copy structure from `account/` or `payment/`)
2. Add core dependency in `pom.xml`:
   ```xml
   <dependency>
       <groupId>com.boilerplate.app</groupId>
       <artifactId>core</artifactId>
       <version>1.0.0</version>
   </dependency>
   ```
3. Configure Eureka client in `application.yml`
4. Add service to `docker/docker-compose.yml`
5. Add route in `config-server/src/main/resources/config-repo/service-gateway.yml`

## Health Checks

All services expose health endpoints:
```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8999/actuator/health
```

## What's Included

- ✅ Service discovery (Eureka)
- ✅ API Gateway with routing and auth
- ✅ OAuth2 authentication
- ✅ Centralized configuration
- ✅ Distributed tracing (end-to-end)
- ✅ Metrics collection (Prometheus)
- ✅ Log aggregation (Loki)
- ✅ Circuit breaker (Resilience4j in Gateway)
- ✅ Structured JSON logging
- ✅ Docker setup

## Notes

- All services log in JSON format with trace IDs
- Logs are automatically collected by Promtail and sent to Loki
- Traces include database queries (JDBC spans)
- Gateway uses circuit breaker to prevent cascading failures
- Config server allows updating configs without restarting services

## Troubleshooting

**Services won't start?**
- Check if Eureka is running: http://localhost:8761
- Check if PostgreSQL is running: `docker-compose ps postgresql`
- Check logs: `docker-compose logs <service-name>`

**Can't see traces?**
- Make sure all services are running
- Check Grafana Tempo data source is configured
- Verify trace IDs are consistent across services

**Logs not showing in Loki?**
- Check Promtail is running: `docker-compose ps promtail`
- Verify Promtail can access Docker logs: `docker-compose logs promtail`

---

**Built with Spring Boot and Spring Cloud**
