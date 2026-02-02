# Eleanor Service Authentication

Authentication service for the Eleanor platform providing OAuth 2.0 token management, partner management, and B2B authentication with HMAC/RSA signature validation.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Project Structure](#project-structure)
- [Business Logic](#business-logic)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [Setup and Running](#setup-and-running)
- [Testing](#testing)

## Overview

The Authentication Service is a Spring Boot microservice that provides:

- **OAuth 2.0 Token Generation**: Client Credentials flow for partners using Basic Authentication
- **B2B Authentication**: RSA/HMAC signature-based authentication for B2B partners
- **Partner Management**: CRUD operations for partners and API keys
- **Token Validation & Revocation**: JWT token validation and revocation services

## Features

- OAuth 2.0 Client Credentials grant flow with Basic Authentication
- RSA signature validation for B2B authentication
- JWT token generation, validation, and revocation
- Partner management with integrated API key fields
- X-PARTNER-ID support in B2B token generation (stored in JWT claims)
- PostgreSQL-based data persistence
- Eureka service discovery integration
- Swagger/OpenAPI documentation

## Project Structure

```
eleanor-service-authentication/
├── src/
│   ├── main/
│   │   ├── java/com/gpay/eleanor/
│   │   │   ├── config/                 # Configuration classes
│   │   │   │   └── SwaggerConfig.java
│   │   │   ├── controller/             # REST controllers
│   │   │   │   ├── OAuth2Controller.java           # OAuth2 endpoints
│   │   │   │   ├── AuthenticationB2BController.java # B2B authentication endpoints
│   │   │   │   └── PartnerController.java          # Partner management endpoints
│   │   │   ├── service/                # Business logic services
│   │   │   │   ├── oauth/              # OAuth2 services
│   │   │   │   │   └── OAuth2TokenService.java
│   │   │   │   ├── partner/            # Partner services
│   │   │   │   │   ├── PartnerService.java
│   │   │   │   ├── b2b/                # B2B services
│   │   │   │   │   ├── B2BTokenService.java
│   │   │   │   │   ├── B2BAccessTokenService.java
│   │   │   │   │   ├── B2BSignatureHelperService.java
│   │   │   │   │   └── B2BSignatureCryptoService.java
│   │   │   │   └── TokenAuthenticationService.java # Shared token service
│   │   │   ├── model/                  # Data models
│   │   │   │   ├── entity/             # JPA entities
│   │   │   │   │   ├── Partner.java
│   │   │   │   │   └── AuthToken.java
│   │   │   │   ├── dto/                # Data Transfer Objects
│   │   │   │   ├── request/            # Request models
│   │   │   │   └── response/           # Response models
│   │   │   ├── repository/             # JPA repositories
│   │   │   │   ├── PartnerRepository.java
│   │   │   │   └── AuthTokenRepository.java
│   │   │   └── util/                   # Utility classes
│   │   │       ├── BasicAuthValidator.java
│   │   │       ├── ErrorCodeConstants.java
│   │   │       └── ResponseHelper.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-local.yml    # Local environment config
│   │       └── application-development.yml
│   └── test/                            # Test files
├── pom.xml
└── README.md
```

## Business Logic

### 1. OAuth 2.0 Token Generation

**Endpoint**: `POST /api/oauth/token`

**Flow**:
1. Client sends Basic Authentication header with `client_id:client_secret`
2. Service validates credentials against partners in PostgreSQL database
3. Validates `grant_type=client_credentials` in request body
4. Finds partner by `partnerCode` or `apiKey` matching `clientId` and verifies `clientSecret`
5. Checks if partner is active
6. Generates JWT token using `TokenAuthenticationService`
7. Returns OAuth2 standard response with `access_token`, `token_type`, `expires_in`, and `scope`

**Key Points**:
- Database lookup for credentials (validates against partners table)
- Supports any active partner with valid `clientSecret`
- Uses Basic Authentication (Base64 encoded `client_id:client_secret`)
- `clientId` can be either `partnerCode` or `apiKey`

### 2. B2B Token Generation

**Endpoint**: `POST /openapi/v1.0/access-token/b2b`

**Flow**:
1. Client sends RSA signature in `X-SIGNATURE` header
2. Client sends `X-CLIENT-KEY` header (API key for partner lookup)
3. Client optionally sends `X-PARTNER-ID` header (will be stored in JWT)
4. Service validates signature using `X-CLIENT-KEY` and `X-TIMESTAMP`
5. Looks up partner by API key in database
6. Validates partner is active
7. Verifies RSA signature matches expected signature
8. Generates JWT token for the partner (includes X-PARTNER-ID if provided)
9. Returns access token response

**JWT Claims**:
- `partnerId`: Partner's database ID
- `X-INTERNAL-PARTNER-ID`: Partner code
- `X-PARTNER-ID`: Partner ID from request header (if provided)

**Key Points**:
- Requires RSA signature validation
- Database lookup for partners by API key
- X-PARTNER-ID is optional and stored in JWT for gateway forwarding

### 3. Token Validation

**Endpoints**: 
- `POST /api/oauth/token/validate` (OAuth2)
- `POST /openapi/v1.0/access-token/validate` (B2B)

**Flow**:
1. Client sends JWT token in request body
2. Service validates JWT signature and expiration
3. Checks if token exists in database and is not revoked
4. Returns validation result (true/false)

### 4. Partner Management

**Endpoints**: `GET/POST/PUT /partners/*`

**Flow**:
- Create partners with integrated API key fields (apiKey, publicKey, privateKey)
- API keys are stored directly in the Partner document
- Activate/deactivate partners
- All data persisted in PostgreSQL

**Partner Document Fields**:
- `partnerCode`, `partnerName`, `clientSecret`
- `apiKey`, `publicKey`, `privateKey`, `apiKeyExpiresAt` (API key fields)
- `partnerPublicKey`, `paymentNotifyUrl`
- `active`, `createdBy`, `createdAt`, `updatedAt`

## Configuration

### Application Properties

Key configuration in `application-local.yml`:

```yaml
server:
  port: 8081

spring:
  data:
  datasource:
    url: jdbc:postgresql://localhost:5432/app_db
    username: app_user
    password: app_password
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

jwt:
  secret: your-jwt-secret-key-minimum-256-bits
  expiration: 3600

oauth:
  token:
    expires-in: 3600
    scope: resource.WRITE resource.READ

```

### Environment Variables

- `SPRING_PROFILES_ACTIVE`: Active profile (local, development, production)
- PostgreSQL connection details
- Eureka server URL

## API Endpoints

### OAuth 2.0 Endpoints

#### Generate Access Token

**Endpoint**: `POST /api/oauth/token`

**Description**: Generates OAuth 2.0 access token using Client Credentials grant flow with Basic Authentication.

**Headers**:
- `Authorization`: `Basic <base64(client_id:client_secret)>`
- `Content-Type`: `application/x-www-form-urlencoded`

**Request Body** (form-urlencoded):
```
grant_type=client_credentials
```

**Response**:
```json
{
    "access_token": "eyJhbGciOiJIUzUxMiJ9...",
    "token_type": "Bearer",
    "expires_in": "3600",
    "scope": "resource.WRITE resource.READ"
}
```

**cURL Example**:
```bash
# Encode credentials: mv9KHahSYg8ZK1DJmS7MboMSsIpBKpVd:VdhiqJB4Z1JEtDPCt7CFvJkNtoepwB09
curl -X POST "http://localhost:8081/api/oauth/token" \
  -H "Authorization: Basic $(echo -n 'mv9KHahSYg8ZK1DJmS7MboMSsIpBKpVd:VdhiqJB4Z1JEtDPCt7CFvJkNtoepwB09' | base64)" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials"
```

#### Validate Token

**Endpoint**: `POST /api/oauth/token/validate`

**Description**: Validates if an OAuth2 JWT token is valid and not revoked.

**Request Body**:
```json
{
    "token": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Response**:
```json
{
    "valid": true
}
```

**cURL Example**:
```bash
curl -X POST "http://localhost:8081/api/oauth/token/validate" \
  -H "Content-Type: application/json" \
  -d '{
    "token": "eyJhbGciOiJIUzUxMiJ9.eyJpYXQiOjE3NjYwMzAxNzIsImV4cCI6MTc2NjAzMzc3Mn0..."
  }'
```

### Partner Management Endpoints

#### Create Partner

**Endpoint**: `POST /partners`

**Request Body**:
```json
{
    "partnerCode": "TEST_PARTNER",
    "partnerName": "Test Partner",
    "clientSecret": "client-secret",
    "apiKey": "api-key-123",
    "publicKey": "-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----",
    "privateKey": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----",
    "partnerPublicKey": "partner-public-key",
    "paymentNotifyUrl": "https://partner.id/notify"
}
```

**cURL Example**:
```bash
curl -X POST "http://localhost:8081/partners" \
  -H "Content-Type: application/json" \
  -d '{
    "partnerCode": "TEST_PARTNER",
    "partnerName": "Test Partner",
    "clientSecret": "client-secret",
    "apiKey": "api-key-123",
    "publicKey": "-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----",
    "privateKey": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----"
  }'
```

**Note**: `apiKey` is optional. If not provided, it will be auto-generated as a UUID.

#### Get All Partners

**Endpoint**: `GET /partners`

**cURL Example**:
```bash
curl -X GET "http://localhost:8081/partners"
```

#### Get Partner by Code

**Endpoint**: `GET /partners/{partnerCode}`

**cURL Example**:
```bash
curl -X GET "http://localhost:8081/partners/TEST_PARTNER"
```

#### Update Partner

**Endpoint**: `PUT /partners/{partnerCode}`

**Request Body** (can update API key fields):
```json
{
    "apiKey": "updated-api-key",
    "publicKey": "-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----",
    "privateKey": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----",
    "apiKeyExpiresAt": "2025-12-31T23:59:59"
}
```

**cURL Example**:
```bash
curl -X PUT "http://localhost:8081/partners/TEST_PARTNER" \
  -H "Content-Type: application/json" \
  -d '{
    "apiKey": "updated-api-key",
    "publicKey": "-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----",
    "privateKey": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----"
  }'
```

**Note**: API keys are now part of the Partner document. Use PUT endpoint to update them.

### B2B Authentication Endpoints

#### Generate B2B Access Token

**Endpoint**: `POST /openapi/v1.0/access-token/b2b`

**Headers**:
- `X-TIMESTAMP`: ISO-8601 timestamp with timezone (required)
- `X-CLIENT-KEY`: Client ID (API Key) (required)
- `X-SIGNATURE`: Base64 encoded RSA signature (required)
- `X-PARTNER-ID`: Partner ID to include in JWT (optional)
- `Content-Type`: `application/json`

**Request Body**:
```json
{
    "grantType": "client_credentials"
}
```

**Response**:
```json
{
    "responseCode": "200_73_00",
    "responseMessage": "Success",
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": "3600"
}
```

**cURL Example**:
```bash
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%S%z")
CLIENT_KEY="your-client-key"
PARTNER_ID="partner-id-123"
SIGNATURE="base64-encoded-rsa-signature"

curl -X POST "http://localhost:8081/openapi/v1.0/access-token/b2b" \
  -H "X-TIMESTAMP: $TIMESTAMP" \
  -H "X-CLIENT-KEY: $CLIENT_KEY" \
  -H "X-PARTNER-ID: $PARTNER_ID" \
  -H "X-SIGNATURE: $SIGNATURE" \
  -H "Content-Type: application/json" \
  -d '{
    "grantType": "client_credentials"
  }'
```

**Note**: The `X-PARTNER-ID` header value (if provided) will be stored in the JWT token as a claim. The gateway will extract this and forward it as `X-INTERNAL-PARTNER-ID` header to B2B APIs.

#### Validate B2B Token

**Endpoint**: `POST /openapi/v1.0/access-token/validate`

**Request Body**:
```json
{
    "token": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**cURL Example**:
```bash
curl -X POST "http://localhost:8081/openapi/v1.0/access-token/validate" \
  -H "Content-Type: application/json" \
  -d '{
    "token": "eyJhbGciOiJIUzUxMiJ9..."
  }'
```

#### Revoke Token

**Endpoint**: `POST /openapi/v1.0/access-token/revoke`

**Request Body**:
```json
{
    "token": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**cURL Example**:
```bash
curl -X POST "http://localhost:8081/openapi/v1.0/access-token/revoke" \
  -H "Content-Type: application/json" \
  -d '{
    "token": "eyJhbGciOiJIUzUxMiJ9..."
  }'
```

## Setup and Running

### Prerequisites

- Java 21+
- Maven 3.6+
- PostgreSQL (running and accessible)
- Eureka Server (optional, for service discovery)

### Local Development Setup

1. **Clone the repository**

2. **Configure PostgreSQL**
   - Ensure PostgreSQL is running on `localhost:5432`
   - Database: `app_db`
   - Username: `app_user`
   - Password: `app_password`

3. **Update configuration** (if needed)
   - Edit `src/main/resources/application-local.yml`
   - Partners are managed through the Partner Management API endpoints
   - Create partners using `POST /partners` endpoint

4. **Build the project**
   ```bash
   mvn clean install
   ```

5. **Run the service**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

   Or using Java directly:
   ```bash
   java -jar target/eleanor-service-authentication-1.0.0.jar --spring.profiles.active=local
   ```

6. **Verify service is running**
   - Health check: `http://localhost:8081/actuator/health`
   - Swagger UI: `http://localhost:8081/swagger-ui.html`
   - API Docs: `http://localhost:8081/api-docs`

### Docker Deployment

```bash
docker build -t eleanor-service-authentication .
docker run -p 8081:8081 \
  -e SPRING_PROFILES_ACTIVE=local \
  eleanor-service-authentication
```

## Testing

### Health Check

```bash
curl http://localhost:8081/actuator/health
```

### Complete OAuth2 Flow Example

1. **Generate Token**:
   ```bash
   TOKEN_RESPONSE=$(curl -s -X POST "http://localhost:8081/api/oauth/token" \
     -H "Authorization: Basic $(echo -n 'mv9KHahSYg8ZK1DJmS7MboMSsIpBKpVd:VdhiqJB4Z1JEtDPCt7CFvJkNtoepwB09' | base64)" \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -d "grant_type=client_credentials")
   
   ACCESS_TOKEN=$(echo $TOKEN_RESPONSE | jq -r '.access_token')
   echo "Access Token: $ACCESS_TOKEN"
   ```

2. **Validate Token**:
   ```bash
   curl -X POST "http://localhost:8081/api/oauth/token/validate" \
     -H "Content-Type: application/json" \
     -d "{\"token\": \"$ACCESS_TOKEN\"}"
   ```

### Testing Partner Management

1. **Create Partner**:
   ```bash
   curl -X POST "http://localhost:8081/partners" \
     -H "Content-Type: application/json" \
     -d '{
       "partnerCode": "TEST_PARTNER",
       "partnerName": "Test Partner",
       "authenticationMethod": "RSA"
     }'
   ```

2. **Update Partner with API Key**:
   ```bash
   curl -X PUT "http://localhost:8081/partners/TEST_PARTNER" \
     -H "Content-Type: application/json" \
     -d '{
       "apiKey": "test-api-key",
       "publicKey": "-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----",
       "privateKey": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----"
     }'
   ```

## Technology Stack

- **Framework**: Spring Boot 3.5.7
- **Language**: Java 21
- **Database**: PostgreSQL
- **Security**: JWT (JJWT 0.12.3)
- **Service Discovery**: Netflix Eureka
- **Documentation**: SpringDoc OpenAPI 3
- **Build Tool**: Maven

## Architecture Notes

### Service Organization

Services are organized by controller:
- `service/oauth/` - OAuth2 services (used by OAuth2Controller)
- `service/partner/` - Partner management services (used by PartnerController)
- `service/b2b/` - B2B authentication services (used by AuthenticationB2BController)
- `service/` (root) - Shared services (TokenAuthenticationService)

### Authentication Methods

1. **OAuth2**: Basic Authentication → JWT Token
   - Credentials validated against partners in PostgreSQL database
   - Supports any active partner with valid clientSecret

2. **B2B (RSA)**: Signature-based authentication
   - API keys stored directly in Partner entity (PostgreSQL)
   - RSA signature validation required
   - Supports X-PARTNER-ID header for JWT token enrichment

### Token Storage

- JWT tokens are stored in PostgreSQL (`authentication.auth_tokens` table)
- Tokens can be validated and revoked
- Token expiration is managed via JWT expiration and database records

### JWT Token Claims

B2B tokens include the following claims:
- `partnerId`: Partner's database ID
- `X-INTERNAL-PARTNER-ID`: Partner code
- `X-PARTNER-ID`: Partner ID from request header (if provided in token generation)

The gateway extracts `X-PARTNER-ID` from JWT and forwards it as `X-INTERNAL-PARTNER-ID` header to B2B APIs.

## Error Responses

All error responses follow a consistent format:

```json
{
    "responseCode": "401_00",
    "responseMessage": "Invalid client credentials",
    "data": {}
}
```

Common HTTP status codes:
- `200`: Success
- `400`: Bad Request (invalid input)
- `401`: Unauthorized (invalid credentials)
- `403`: Forbidden (invalid partner credentials)
- `404`: Not Found
- `500`: Internal Server Error
