# API Gateway - Project Structure & Code Guide

## Table of Contents
1. [Overview](#overview)
2. [Project Structure](#project-structure)
3. [Component Details](#component-details)
4. [Request Flow](#request-flow)
5. [Configuration](#configuration)
6. [Authentication](#authentication)
7. [Development Guide](#development-guide)

---

## Overview

The Gateway Service is a Spring Boot microservice that provides:

- **Request Routing**: Routes requests to appropriate backend services based on path patterns
- **Token Authentication**: Validates JWT tokens using strategy pattern (OAuth2 and B2B)
- **Header Forwarding**: Extracts and forwards custom headers (X-INTERNAL-PARTNER-ID) from JWT to backend services
- **Load Balancing**: Automatic service discovery and load balancing via Eureka

Tech Stack:
- **Spring Cloud LoadBalancer** for service discovery and load balancing
- **Eureka** for service registry
- **RestTemplate** (non-reactive) for HTTP communication
- **Spring Boot Actuator** for health checks
- **JWT (JJWT)** for token introspection and claim extraction

---

## Project Structure

```
eleanor-service-gateway/
├── src/main/java/com/gpay/eleanor/
│   ├── config/                          # Configuration classes
│   │   ├── GatewayConfig.java          # RestTemplate & LoadBalancer setup
│   │   ├── GatewayRouteConfig.java     # Route configuration loader
│   │   └── SwaggerConfig.java          # Swagger configuration
│   │
│   ├── controller/                      # REST Controllers
│   │   └── GatewayController.java      # Main gateway routing endpoint
│   │
│   ├── service/                         # Business logic services
│   │   └── GatewayService.java         # Request forwarding implementation
│   │
│   ├── filter/                          # Servlet filters
│   │   ├── CachedBodyFilter.java       # Request body caching filter
│   │   └── AuthenticationFilter.java   # Token authentication filter
│   │
│   ├── validator/                       # Token validation strategies
│   │   ├── ITokenValidator.java        # Token validator interface
│   │   ├── TokenValidatorRouter.java   # Validator router/resolver
│   │   ├── OAuth2TokenValidator.java   # OAuth2 token validator
│   │   └── B2BTokenValidator.java      # B2B token validator
│   │
│   ├── model/                           # Data models
│   │   ├── ServiceRoute.java           # Route configuration DTO
│   │   └── EndpointRoute.java          # Endpoint route DTO
│   │
│   └── EleanorServiceGatewayApplication.java  # Main application class
│
└── src/main/resources/
    ├── application.yml                  # Base configuration
    ├── application-default.yml          # Default profile configuration
    └── application-development.yml      # Development profile configuration
```

---

## Component Details

### 1. Configuration Layer

#### `GatewayConfig.java`
**Purpose**: Configures RestTemplate with LoadBalancer support for service discovery.

**Key Features**:
- Creates `@LoadBalanced` RestTemplate bean
- Enables automatic service name resolution via Eureka
- Configures HTTP timeouts (5s connect, 30s read)

**Usage**: Automatically injected into services that need to call backend services.

---

#### `GatewayRouteConfig.java`
**Purpose**: Loads route configurations from `application.yml`.

**Configuration Example**:
```yaml
gateway:
  routes:
    authentication-service:
      path: /auth
      service-id: eleanor-service-authentication
      strip-prefix: true
      enabled: true
```

**Fields**:
- `path`: URL pattern to match (e.g., `/auth`)
- `service-id`: Eureka service name
- `strip-prefix`: Remove path prefix when forwarding
- `enabled`: Enable/disable route

---

### 2. Controller Layer

#### `GatewayController.java`
**Purpose**: Main entry point for all gateway requests. Handles routing logic.

**Endpoint**: `POST/GET/PUT/DELETE/PATCH /api/**`

**Responsibilities**:
1. Receives all requests to `/api/**`
2. Removes `/api` prefix
3. Finds matching route configuration
4. Builds target path (with/without prefix stripping)
5. Preserves query parameters
6. Extracts request body
7. Forwards to `GatewayService`

**Key Methods**:
- `route()`: Main routing method
- `findMatchingRoute()`: Finds route using longest prefix matching
- `extractRequestBody()`: Extracts body from request or cache

---

### 3. Service Layer

#### `GatewayService.java` & `GatewayServiceImpl.java`
**Purpose**: Handles actual HTTP forwarding to backend services.

**Responsibilities**:
1. Builds target URL using service ID
2. Copies HTTP headers (excluding connection-specific ones)
3. Forwards request via RestTemplate
4. Handles errors and forwards responses

**Key Methods**:
- `routeRequest()`: Forwards request to backend service
- `copyHeaders()`: Copies and filters headers

**Error Handling**:
- HTTP errors (4xx, 5xx): Forwards error response
- Connection errors: Returns 500 with error message

---

---

### 4. Filter Layer

#### `CachedBodyFilter.java`
**Purpose**: Caches request body for gateway requests.

**Why Needed**: 
- Request body can only be read once from input stream
- Needed for both `@RequestBody` annotation and forwarding

**Behavior**:
- Wraps requests with `ContentCachingRequestWrapper`
- Allows multiple reads of request body

#### `AuthenticationFilter.java`
**Purpose**: Validates JWT tokens and extracts claims for header forwarding.

**Responsibilities**:
1. Intercepts requests to routes that require authentication
2. Extracts JWT token from `Authorization: Bearer <token>` header
3. Uses `TokenValidatorRouter` to select appropriate validator (OAuth2 or B2B)
4. Validates token via validator's validation endpoint
5. Extracts `X-PARTNER-ID` from B2B tokens and sets as `X-INTERNAL-PARTNER-ID` request attribute
6. Bypasses authentication for Swagger/documentation endpoints

**Order**: Runs after `CachedBodyFilter` (Order 2)

---

### 5. Token Validation Layer

#### `ITokenValidator.java`
**Purpose**: Interface for token validation strategies.

**Methods**:
- `validate(String token, ServiceRoute route)`: Validates token
- `extractAdditionalInfo(String token)`: Extracts additional info (legacy, returns null for B2B)
- `supports(ServiceRoute route)`: Checks if validator supports route
- `getValidationEndpoint()`: Returns validation endpoint URL

#### `TokenValidatorRouter.java`
**Purpose**: Resolves the appropriate token validator for a given route.

**Behavior**:
- Iterates through registered validators
- Returns first validator that `supports()` the route
- Defaults to `OAuth2TokenValidator` if no specific validator found

#### `OAuth2TokenValidator.java`
**Purpose**: Validates OAuth2 tokens for routes starting with `/api`.

**Validation Endpoint**: `http://eleanor-service-authentication/api/oauth/token/validate`

**Supports**: Routes with `base-path` starting with `/api`

#### `B2BTokenValidator.java`
**Purpose**: Validates B2B tokens and extracts `X-PARTNER-ID` from JWT.

**Validation Endpoint**: `http://eleanor-service-authentication/openapi/v1.0/access-token/validate`

**Supports**: Routes with `base-path` starting with `/qris-partner`, `/openapi`, or `/b2b`

**Methods**:
- `extractPartnerId(String token)`: Extracts `X-PARTNER-ID` claim from JWT

---

### 6. Health Check Layer

#### `GatewayHealthIndicator.java`
**Purpose**: Provides health status for the gateway.

**Endpoint**: `GET /actuator/health`

**Behavior**:
- Returns simple health status: `UP` with service name
- Simplified to match other services' health check pattern

---

### 7. Model Layer

#### `ServiceRoute.java`
**Purpose**: DTO representing route configuration.

**Fields**:
- `basePath`: Base path pattern to match (e.g., `/api`, `/qris-partner`)
- `serviceId`: Eureka service name
- `stripPrefix`: Whether to strip base path prefix when forwarding
- `requiredAuth`: Whether authentication is required for this route
- `enabled`: Route enabled flag
- `endpoints`: List of endpoint-specific routes (optional)

#### `EndpointRoute.java`
**Purpose**: DTO representing endpoint-specific route configuration.

**Fields**:
- `path`: Endpoint path pattern
- `targetPath`: Target path for forwarding (optional)
- `enabled`: Endpoint enabled flag

---

## Request Flow

### Complete Request Flow Diagram

```
┌─────────────┐
│   Client    │
│  (Browser/  │
│   Mobile)   │
└──────┬──────┘
       │
       │ 1. HTTP Request
       │    GET /api/auth/users
       │
       ▼
┌─────────────────────────────────────┐
│   CachedBodyFilter                  │
│   - Wraps request with              │
│     ContentCachingRequestWrapper    │
│   - Caches request body             │
└──────────────┬──────────────────────┘
               │
               │ 2. Filtered Request
               │
               ▼
┌─────────────────────────────────────┐
│   GatewayController                 │
│   - Receives: /api/auth/users       │
│   - Removes prefix: /auth/users     │
│   - Finds route: /auth →            │
│     eleanor-service-authentication  │
│   - Strips prefix: /users           │
│   - Extracts body & headers         │
└──────────────┬──────────────────────┘
               │
               │ 3. Route Request
               │    serviceId: eleanor-service-authentication
               │    path: /users
               │
               ▼
┌─────────────────────────────────────┐
│   GatewayServiceImpl                │
│   - Builds URL:                     │
│     http://eleanor-service-         │
│     authentication/users            │
│   - Copies headers                  │
│   - Creates HttpEntity              │
└──────────────┬──────────────────────┘
               │
               │ 4. HTTP Request via RestTemplate
               │    (LoadBalanced)
               │
               ▼
┌─────────────────────────────────────┐
│   Spring Cloud LoadBalancer         │
│   - Resolves service name           │
│   - Looks up in Eureka              │
│   - Gets actual URL:                │
│     http://192.168.1.100:8080       │
│   - Load balances if multiple       │
│     instances exist                 │
└──────────────┬──────────────────────┘
               │
               │ 5. HTTP Request
               │    GET http://192.168.1.100:8080/users
               │
               ▼
┌─────────────────────────────────────┐
│   Backend Service                   │
│   (eleanor-service-authentication)  │
│   - Processes request               │
│   - Returns response                │
└──────────────┬──────────────────────┘
               │
               │ 6. HTTP Response
               │
               ▼
┌─────────────────────────────────────┐
│   GatewayServiceImpl                │
│   - Receives response               │
│   - Logs status                     │
└──────────────┬──────────────────────┘
               │
               │ 7. Response
               │
               ▼
┌─────────────────────────────────────┐
│   GatewayController                 │
│   - Returns response to client      │
└──────────────┬──────────────────────┘
               │
               │ 8. HTTP Response
               │
               ▼
┌─────────────┐
│   Client    │
└─────────────┘
```

### Step-by-Step Flow Explanation

#### Step 1: Request Arrives
```
Client → GET /api/auth/users?page=1
```

#### Step 2: Filter Processing
- `CachedBodyFilter` intercepts request
- Wraps with `ContentCachingRequestWrapper`
- Allows body to be read multiple times

#### Step 3: Authentication Filter
- `AuthenticationFilter` intercepts request
- Finds matching route configuration
- If `required-auth: true`:
  - Extracts JWT token from `Authorization: Bearer <token>` header
  - Uses `TokenValidatorRouter` to select validator (OAuth2 or B2B)
  - Validates token via validator's endpoint
  - For B2B tokens: Extracts `X-PARTNER-ID` from JWT claims
  - Sets `X-INTERNAL-PARTNER-ID` request attribute
- Skips authentication for Swagger/documentation paths

#### Step 4: Controller Processing
- `GatewayController.route()` receives request
- Calls `findMatchingRoute()` to find route configuration
- Finds route: `base-path=/qris-partner`, `service-id=eleanor-service-qris-partner`
- Builds target path (respects `strip-prefix` and endpoint routes)
- Appends query string if present
- Extracts request body

#### Step 5: Service Forwarding
- `GatewayService.routeRequest()` called
- Builds URL: `http://eleanor-service-qris-partner/qris-partner/v1/payment`
- Copies headers (excludes host, connection, transfer-encoding)
- Adds `X-INTERNAL-PARTNER-ID` header from request attribute (if present)
- Creates `HttpEntity` with body and headers

#### Step 6: LoadBalancer Resolution
- `@LoadBalanced` RestTemplate intercepts
- Resolves service name via Eureka
- Gets actual service URL (e.g., `http://192.168.1.100:8080`)
- If multiple instances: load balances

#### Step 7: Backend Service
- Backend service receives request with `X-INTERNAL-PARTNER-ID` header
- Processes and returns response

#### Step 8-9: Response Return
- Response flows back through gateway
- Gateway returns to client

---

## Configuration

### Route Configuration

Routes are configured in `application-default.yml`:

```yaml
gateway:
  routes:
    route-name:
      path: /path-pattern          # URL pattern to match
      service-id: service-name     # Eureka service ID
      strip-prefix: true           # Remove path prefix
      enabled: true                # Enable/disable route
```

### Example Routes

```yaml
gateway:
  routes:
    # Route 1: Authentication service with prefix stripping
    authentication-service:
      path: /auth
      service-id: eleanor-service-authentication
      strip-prefix: true
      enabled: true
    # Request: /api/auth/users → Backend: /users
    
    # Route 2: Same service without prefix stripping
    authentication-service-service:
      path: /service
      service-id: eleanor-service-authentication
      strip-prefix: false
      enabled: true
    # Request: /api/service/users → Backend: /service/users
```

### Route Matching Rules

1. **Longest Prefix Match**: If multiple routes match, the longest path wins
   - Routes: `/auth` and `/auth/users`
   - Path: `/auth/users/profile`
   - Matches: `/auth/users` (longer match)

2. **Enabled Routes Only**: Disabled routes are ignored

3. **Path Must Start With**: Route path must be a prefix of request path

---

## Authentication

The gateway implements token-based authentication using a strategy pattern to support multiple token types (OAuth2 and B2B).

### Token Validation Strategy Pattern

The gateway uses a strategy pattern to validate different types of tokens:

#### Components

1. **ITokenValidator** (Interface)
   - Defines contract for token validation
   - Methods: `validate()`, `extractAdditionalInfo()`, `supports()`, `getValidationEndpoint()`

2. **TokenValidatorRouter**
   - Resolves the appropriate validator based on route configuration
   - Iterates through registered validators and returns the first that `supports()` the route

3. **OAuth2TokenValidator**
   - Validates OAuth2 tokens
   - Validation endpoint: `http://eleanor-service-authentication/api/oauth/token/validate`
   - Supports routes with `base-path` starting with `/api`

4. **B2BTokenValidator**
   - Validates B2B tokens
   - Validation endpoint: `http://eleanor-service-authentication/openapi/v1.0/access-token/validate`
   - Supports routes with `base-path` starting with `/qris-partner`, `/openapi`, or `/b2b`
   - Extracts `X-PARTNER-ID` from JWT claims

### Authentication Flow

#### 1. Request with Token

```
Client → GET /qris-partner/v1/payment
Headers:
  Authorization: Bearer <jwt-token>
```

#### 2. AuthenticationFilter Processing

1. **Route Matching**: Finds matching route configuration
2. **Auth Check**: If `required-auth: true`:
   - Extracts token from `Authorization: Bearer <token>` header
   - Uses `TokenValidatorRouter` to select validator
   - Calls validator's `validate()` method
   - For B2B tokens: Calls `extractPartnerId()` to get `X-PARTNER-ID` from JWT
   - Sets `X-INTERNAL-PARTNER-ID` request attribute

3. **Bypass**: Skips authentication for Swagger/documentation paths

#### 3. GatewayService Header Forwarding

- Checks for `X-INTERNAL-PARTNER-ID` request attribute
- Adds it as header to forwarded request:
  ```
  X-INTERNAL-PARTNER-ID: <value-from-jwt>
  ```

### B2B Token Flow

#### Token Generation (Authentication Service)

When a partner requests a B2B token:
```
POST /openapi/v1.0/access-token/b2b
Headers:
  X-CLIENT-KEY: <api-key>
  X-PARTNER-ID: <partner-id>  (optional)
  X-SIGNATURE: <rsa-signature>
  X-TIMESTAMP: <timestamp>
```

The authentication service:
1. Validates signature and credentials
2. Generates JWT token with claims:
   - `partnerId`: Partner's database ID
   - `X-INTERNAL-PARTNER-ID`: Partner code
   - `X-PARTNER-ID`: Partner ID from request header (if provided)

#### Token Usage (Gateway)

When a client uses the token:
```
GET /qris-partner/v1/payment
Headers:
  Authorization: Bearer <jwt-token>
```

The gateway:
1. Validates token using `B2BTokenValidator`
2. Extracts `X-PARTNER-ID` from JWT claims
3. Forwards request to backend service with:
   ```
   X-INTERNAL-PARTNER-ID: <extracted-partner-id>
   ```

### Route Configuration for Authentication

#### Example: B2B Route with Authentication

```yaml
gateway:
  routes:
    qris-partner-service:
      base-path: /qris-partner
      service-id: eleanor-service-qris-partner
      strip-prefix: false
      enabled: true
      required-auth: true  # Triggers AuthenticationFilter
      endpoints: []
```

#### Example: Public Route (No Authentication)

```yaml
gateway:
  routes:
    artajasa-service:
      base-path: /artajasa
      service-id: eleanor-service-artajasa
      strip-prefix: false
      enabled: true
      required-auth: false  # Skips AuthenticationFilter
      endpoints: []
```

### Swagger/Documentation Bypass

The `AuthenticationFilter` automatically bypasses authentication for:
- `/swagger-ui*`
- `/api-docs*`
- `/v3/api-docs*`
- `/swagger-resources*`
- `/webjars/springfox-swagger-ui*`

This allows Swagger UI and API documentation to be publicly accessible.

### Authentication Error Handling

#### Missing Token
```
HTTP 401 Unauthorized
{
  "error": "Unauthorized",
  "message": "Missing or invalid Authorization header",
  "status": 401
}
```

#### Invalid Token
```
HTTP 401 Unauthorized
{
  "error": "Unauthorized",
  "message": "Invalid or expired token",
  "status": 401
}
```

#### No Validator Available
```
HTTP 401 Unauthorized
{
  "error": "Unauthorized",
  "message": "No token validator available for this route",
  "status": 401
}
```

---

## Development Guide

### Adding a New Route

1. **Edit `application-default.yml`**:
```yaml
gateway:
  routes:
    new-service:
      path: /new
      service-id: eleanor-service-new
      strip-prefix: true
      enabled: true
```

2. **Ensure service is registered in Eureka**:
   - Service must have `spring.application.name=eleanor-service-new`
   - Service must be registered with Eureka server

3. **Test the route**:
```bash
curl http://localhost:8999/api/new/endpoint
```

### Adding Custom Logic

#### Modify Request Before Forwarding

Edit `GatewayServiceImpl.routeRequest()`:
```java
// Add custom headers
requestHeaders.add("X-Custom-Header", "value");

// Modify request body
if (body != null) {
    String modifiedBody = modifyBody(body);
    requestEntity = new HttpEntity<>(modifiedBody, requestHeaders);
}
```

#### Add Request/Response Logging

Edit `GatewayServiceImpl.routeRequest()`:
```java
log.info("Request body: {}", body);
log.info("Response body: {}", response.getBody());
```

#### Add Authentication/Authorization

Create a new filter or interceptor:
```java
@Component
public class AuthFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(...) {
        // Validate token
        // Add user context to request
    }
}
```

### Health Check Customization

Edit `GatewayHealthIndicator.health()`:
```java
// Add custom health checks
details.put("customCheck", performCustomCheck());

// Modify health status logic
if (allServicesUp && customCheckPassed) {
    return Health.up().withDetails(details).build();
}
```

### Error Handling

#### Custom Error Responses

Edit `GatewayController.route()`:
```java
catch (Exception e) {
    // Custom error format
    String errorResponse = String.format(
        "{\"error\":{\"code\":\"GATEWAY_ERROR\",\"message\":\"%s\"}}", 
        e.getMessage()
    );
    return ResponseEntity.status(500)
        .contentType(MediaType.APPLICATION_JSON)
        .body(errorResponse);
}
```

#### Retry Logic

Add retry mechanism in `GatewayServiceImpl`:
```java
@Retryable(maxAttempts = 3)
public ResponseEntity<String> routeRequest(...) {
    // Retry logic
}
```

---

## Common Patterns

### Pattern 1: Simple Route Forwarding
```yaml
gateway:
  routes:
    service:
      path: /api
      service-id: backend-service
      strip-prefix: true
```
**Result**: `/api/users` → Backend: `/users`

### Pattern 2: Route Without Prefix Stripping
```yaml
gateway:
  routes:
    service:
      path: /api
      service-id: backend-service
      strip-prefix: false
```
**Result**: `/api/users` → Backend: `/api/users`

### Pattern 3: Multiple Routes to Same Service
```yaml
gateway:
  routes:
    route1:
      path: /v1
      service-id: backend-service
      strip-prefix: true
    route2:
      path: /v2
      service-id: backend-service
      strip-prefix: true
```
**Result**: 
- `/v1/users` → Backend: `/users`
- `/v2/users` → Backend: `/users`

---

## Testing

### Test Gateway Route
```bash
# Test GET request
curl http://localhost:8999/api/auth/users

# Test POST request
curl -X POST http://localhost:8999/api/auth/users \
  -H "Content-Type: application/json" \
  -d '{"name":"John"}'

# Test with query parameters
curl http://localhost:8999/api/auth/users?page=1&size=10
```

### Test Health Check
```bash
# Gateway health
curl http://localhost:8999/actuator/health

# Liveness probe
curl http://localhost:8999/actuator/health/liveness

# Readiness probe
curl http://localhost:8999/actuator/health/readiness
```


---

## Troubleshooting

### Route Not Found (404)
- **Check**: Route configuration in `application.yml`
- **Check**: Route is enabled (`enabled: true`)
- **Check**: Path pattern matches request path
- **Check**: Service is registered in Eureka

### Service Unreachable
- **Check**: Service is registered in Eureka
- **Check**: Service health endpoint is accessible
- **Check**: Network connectivity
- **Check**: Service is running

### Request Body Missing
- **Check**: `CachedBodyFilter` is active
- **Check**: Request has `Content-Type` header
- **Check**: Body is not empty

### LoadBalancer Not Resolving
- **Check**: `@LoadBalanced` annotation on RestTemplate
- **Check**: Eureka client is configured
- **Check**: Service name matches Eureka registration

---

## Best Practices

1. **Route Naming**: Use descriptive names (e.g., `authentication-service` not `route1`)
2. **Path Patterns**: Use clear, consistent path patterns
3. **Error Handling**: Always handle errors gracefully
4. **Logging**: Log important events (routing, errors)
5. **Health Checks**: Monitor gateway and downstream services
6. **Configuration**: Keep routes in configuration files, not code
7. **Security**: Add authentication/authorization as needed
8. **Performance**: Monitor response times and optimize

---

## Quick Reference

### Key Classes
- `GatewayController`: Entry point for requests
- `GatewayServiceImpl`: Request forwarding logic
- `GatewayConfig`: RestTemplate configuration
- `GatewayRouteConfig`: Route configuration loader
- `CachedBodyFilter`: Request body caching
- `GatewayHealthIndicator`: Health check

### Key Endpoints
- `/api/**`: Gateway routing endpoint
- `/actuator/health`: Health check endpoint
- `/actuator/health/liveness`: Liveness probe
- `/actuator/health/readiness`: Readiness probe

### Key Configuration
- `gateway.routes.*`: Route definitions
- `eureka.client.*`: Eureka configuration
- `management.endpoints.*`: Actuator configuration

---

## Next Steps

1. Review the code structure
2. Understand the request flow
3. Configure routes for your services
4. Test the gateway with your services
5. Add custom logic as needed
6. Monitor health and performance

