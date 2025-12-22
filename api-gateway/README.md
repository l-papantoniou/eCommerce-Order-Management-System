# API Gateway

Production-ready Spring Cloud Gateway serving as the secure, centralized entry point for the eCommerce Order Management System microservices.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Routes & Security](#routes--security)
- [Testing](#testing)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)

---

## Overview

The API Gateway acts as a single entry point for all client requests to backend microservices. It handles cross-cutting concerns such as authentication, rate limiting, routing, and logging, allowing downstream services to focus on business logic.

### Key Responsibilities:
- **Security** - JWT token validation and user authentication
- **Rate Limiting** - Token bucket algorithm to prevent abuse
- **Routing** - Intelligent path-based routing to microservices
- **Observability** - Request/response logging with correlation IDs
- **Resilience** - Circuit breaker fallback support

---

## Features

### Authentication & Authorization
- **JWT Token Validation** - Validates tokens offline using RSA public keys from JWKS endpoint
- **User Context Extraction** - Extracts user ID, role, and email from tokens
- **Header Propagation** - Forwards user information to downstream services via HTTP headers
- **Proper Error Responses** - Returns 401/403 with JSON error messages

### Rate Limiting
- **Token Bucket Algorithm** - Implemented via Bucket4j with Redis
- **Multiple Strategies** - Per-user, per-IP, per-API-key, or global limiting
- **Configurable Per Route** - Different limits for different services
- **Distributed** - Redis-based storage works across multiple gateway instances
- **Informative Headers** - Returns `X-RateLimit-*` headers with every response

### Observability
- **Request ID Generation** - Unique correlation ID for distributed tracing
- **Detailed Logging** - Request/response logging with duration tracking
- **Slow Request Detection** - Automatic warnings for requests exceeding thresholds
- **Service-Specific Prefixes** - Easy log filtering by service ([ORDER], [ANALYTICS], etc.)

### Routing
- **Path-Based Routing** - Routes requests based on URL patterns
- **Load Balancing Ready** - Prepared for service discovery integration
- **Fallback Support** - Circuit breaker fallback endpoints

### Performance
- **Reactive & Non-Blocking** - Built on Spring WebFlux for high throughput
- **Async Processing** - Handles thousands of concurrent connections
- **Efficient Resource Usage** - Event-loop model minimizes thread overhead

---

## Architecture

### System Context

```
┌─────────────────┐
│     Clients     │
│  (Web/Mobile)   │
└────────┬────────┘
         │ HTTPS
         │ Authorization: Bearer <JWT>
         ↓
┌──────────────────────────────────────┐
│      API Gateway (Port 8090)         │
│                                      │
│  ┌────────────────────────────────┐ │
│  │  Global Filters                │ │
│  │  1. RequestIdFilter            │ │
│  │     → X-Request-Id generation  │ │
│  └────────────────────────────────┘ │
│              ↓                       │
│  ┌────────────────────────────────┐ │
│  │  Route-Specific Filters        │ │
│  │  2. AuthenticationFilter       │ │
│  │     → JWT validation           │ │
│  │     → User extraction          │ │
│  │  3. RateLimitingFilter         │ │
│  │     → Token bucket check       │ │
│  │  4. LoggingFilter              │ │
│  │     → Request/response logs    │ │
│  └────────────────────────────────┘ │
│              ↓                       │
│  ┌────────────────────────────────┐ │
│  │  Route to Microservice         │ │
│  └────────────────────────────────┘ │
└────────┬─────────────────────────────┘
         │ + Headers:
         │   X-Request-Id, X-User-Id,
         │   X-User-Role, X-User-Email
         │
    ┌────┴─────────┬────────────────┐
    ↓              ↓                ↓
┌─────────┐  ┌──────────┐  ┌──────────────┐
│ Order   │  │Analytics │  │Notification  │
│ Service │  │ Service  │  │  Service     │
│ (8080)  │  │ (8082)   │  │  (8081)      │
└─────────┘  └──────────┘  └──────────────┘
```

### Request Flow

```
1. Client sends request with JWT
   POST http://localhost:8090/api/v1/orders
   Authorization: Bearer eyJhbGc...
   
2. RequestIdFilter (Global)
    Generates: X-Request-Id: abc-123-def-456
   
3. AuthenticationFilter
    Validates JWT signature
    Checks expiration
    Extracts claims: userId, role, email
    Adds headers: X-User-Id, X-User-Role, X-User-Email
    Returns 401/403 if invalid
   
4. RateLimitingFilter
    Checks Redis bucket for user:123
    Consumes 1 token (49 remaining)
    Adds headers: X-RateLimit-Limit, X-RateLimit-Remaining
    Returns 429 if exceeded
   
5. LoggingFilter
    Logs: [ORDER] ==> [abc-123] POST /api/v1/orders
   
6. Forward to Order Service
   POST http://localhost:8080/api/v1/orders
   Headers included:
     X-Request-Id: abc-123-def-456
     X-User-Id: user-123
     X-User-Role: ROLE_USER
     X-User-Email: john@example.com
     X-Authenticated: true
   
7. Order Service processes request
   
8. LoggingFilter (Post-processing)
    Logs: [ORDER] <== [abc-123] - Status: 201 - Duration: 523ms
   
9. Response returned to client
   Status: 201 Created
   Headers:
     X-Request-Id: abc-123-def-456
     X-RateLimit-Limit: 50
     X-RateLimit-Remaining: 49
     X-RateLimit-Reset: 1733583600000
```

---

## Prerequisites

Before running the API Gateway, ensure you have:

### Required:
- **Java 21+** - Programming language runtime
- **Maven 3.9+** - Build tool
- **Redis 7.x** - For distributed rate limiting
  ```bash
  docker run -d -p 6379:6379 --name redis redis:alpine
  ```

### For Full Authentication:
- **Authorization Server** - Running on port 9000
    - Must provide JWKS endpoint: `http://localhost:9000/oauth2/jwks`
    - Must issue JWT tokens with RS256 algorithm
    - Issuer claim must be: `http://localhost:9000`

### Backend Services:
- **Order Service** - Port 8080
- **Analytics Service** - Port 8082
- **Notification Service** - Port 8081

---

##  Installation

### 1. Clone the Repository

```bash
cd ecommerce-order-management-system/api-gateway
```

### 2. Build the Project

```bash
mvn clean install
```

### 3. Run the Application

```bash
mvn spring-boot:run
```

The gateway will start on **port 8090**.

---

## ⚙️ Configuration

### Main Configuration File

**Location:** `src/main/resources/application.yml`

```yaml
server:
  port: 8090

spring:
  application:
    name: api-gateway

  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms

  cloud:
    gateway:
      routes:
        # Configuration for each route...

# JWT Configuration
jwt:
  jwks-uri: http://localhost:9000/oauth2/jwks
  issuer: http://localhost:9000

# Rate Limiting Configuration
rate-limit:
  enabled: true
  default-capacity: 100
  default-refill-tokens: 100
  default-refill-duration: 60

logging:
  level:
    com.ecommerce.gateway: DEBUG
```

### Environment-Specific Configuration

You can override properties using environment variables or profiles:

```bash
# Using environment variables
export REDIS_HOST=production-redis.example.com
export JWT_JWKS_URI=https://auth.example.com/oauth2/jwks

# Using Spring profiles
mvn spring-boot:run -Dspring.profiles.active=prod
```

---

## 🔒 Routes & Security

### Protected Routes (Authentication + Rate Limiting)

#### Order Service
```yaml
Path: /api/v1/orders/**
Target: http://localhost:8080
Authentication: Required (JWT)
Rate Limit: 50 requests/min per user
Slow Threshold: 1000ms
Log Prefix: [ORDER]
```

**Example Request:**
```bash
curl -H "Authorization: Bearer <JWT>" \
  http://localhost:8090/api/v1/orders
```

**Response Headers:**
```http
HTTP/1.1 200 OK
X-Request-Id: abc-123-def-456
X-RateLimit-Limit: 50
X-RateLimit-Remaining: 49
X-RateLimit-Reset: 1733583600000
```

#### Analytics Service
```yaml
Path: /api/v1/analytics/**
Target: http://localhost:8082
Authentication: Required (JWT)
Rate Limit: 100 requests/min per user
Slow Threshold: 2000ms
Log Prefix: [ANALYTICS]
```

#### Notification Service
```yaml
Path: /api/v1/notifications/**
Target: http://localhost:8081
Authentication: Required (JWT)
Rate Limit: 30 requests/min per user
Log Prefix: [NOTIFICATION]
```

### Public Routes (Rate Limiting Only)

#### Health Check
```yaml
Path: /actuator/health
Target: http://localhost:8080/actuator/health
Authentication: Not Required
Rate Limit: 10 requests/min per IP
Log Prefix: [HEALTH]
```

**Example Request:**
```bash
curl http://localhost:8090/actuator/health
```

### Error Responses

#### 401 Unauthorized - Missing Token
```json
{
  "error": "Missing authorization header",
  "status": 401,
  "timestamp": "2024-12-07T14:30:00.191199Z"
}
```

#### 403 Forbidden - Invalid Token
```json
{
  "error": "Invalid or expired token",
  "status": 403,
  "timestamp": "2024-12-07T14:30:00.191199Z"
}
```

#### 429 Too Many Requests
```json
{
  "error": "Rate limit exceeded",
  "message": "Too many requests. Please try again in 60 seconds.",
  "status": 429,
  "timestamp": "2024-12-07T14:30:00.191199Z"
}
```

---

## Testing

### Basic Routing Test

```bash
# Test without authentication (should return 401)
curl http://localhost:8090/api/v1/orders

# Expected: 401 Unauthorized
```

### Authenticated Request

```bash
# Get JWT token from your authorization server
TOKEN=$(curl -X POST http://localhost:9000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=your-client-id" \
  -d "client_secret=your-secret" \
  | jq -r '.access_token')

# Make authenticated request
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8090/api/v1/orders

# Expected: 200 OK with data
```

### Rate Limiting Test

```bash
# Make 51 requests (exceeds 50/min limit)
for i in {1..51}; do
  echo "Request $i:"
  curl -s -H "Authorization: Bearer $TOKEN" \
    http://localhost:8090/api/v1/orders \
    | jq -r '.status // .orderId'
done

# Requests 1-50: Success
# Request 51: 429 Too Many Requests
```

### Check Response Headers

```bash
curl -v -H "Authorization: Bearer $TOKEN" \
  http://localhost:8090/api/v1/orders 2>&1 | grep "X-"

# Expected headers:
# X-Request-Id: abc-123-def-456
# X-RateLimit-Limit: 50
# X-RateLimit-Remaining: 49
# X-RateLimit-Reset: 1733583600000
```

---

## Monitoring

### Health Check

```bash
curl http://localhost:8090/actuator/health

# Response:
{
  "status": "UP"
}
```

### View Configured Routes

```bash
curl http://localhost:8090/actuator/gateway/routes | jq

# Shows all configured routes with predicates and filters
```

### Monitor Logs

```bash
# Gateway logs show:

# Request logging:
[ORDER] ==> [abc-123] POST /api/v1/orders

# Authentication:
Authenticated user: user-123 (role: ROLE_USER) for path: /api/v1/orders

# Rate limiting:
Rate limit passed - Key: user:123, Remaining: 49/50

# Response logging:
[ORDER] <== [abc-123] POST /api/v1/orders - Status: 201 - Duration: 523ms

# Slow requests:
[ANALYTICS] SLOW REQUEST <== [def-456] GET /api/v1/analytics/summary - Status: 200 - Duration: 3245ms
```

### Redis Monitoring

```bash
# Connect to Redis
docker exec -it redis redis-cli

# List rate limit keys
KEYS user:*

# Check specific bucket
GET user:123

# Monitor real-time
MONITOR
```

---

## Troubleshooting

### Issue: "Cannot initialize JWT validation"

**Problem:** Authorization Server not accessible

**Solution:**
```bash
# Check if auth server is running
curl http://localhost:9000/oauth2/jwks

# Should return JWKS JSON with public keys
```

### Issue: "Connection refused" to Redis

**Problem:** Redis not running

**Solution:**
```bash
# Start Redis
docker run -d -p 6379:6379 --name redis redis:alpine

# Verify connection
redis-cli ping
# Expected: PONG
```

### Issue: All requests return 429

**Problem:** Rate limit too strict or Redis bucket issue

**Solution:**
```bash
# Clear Redis buckets
docker exec redis redis-cli FLUSHALL

# Or increase capacity in application.yml
rate-limit:
  default-capacity: 200  # Increase from 100
```

### Issue: Rate limiting not working

**Problem:** Rate limiting disabled

**Solution:**
```yaml
# In application.yml, ensure:
rate-limit:
  enabled: true  # Must be true
```

### Issue: Requests bypass authentication

**Problem:** Filter not applied to route

**Solution:**
Check route configuration includes AuthenticationFilter:
```yaml
filters:
  - AuthenticationFilter  # Must be present
```

---

## Project Structure

```
api-gateway/
├── src/
│   ├── main/
│   │   ├── java/com/ecommerce/gateway/
│   │   │   ├── ApiGatewayApplication.java      # Main application
│   │   │   ├── config/
│   │   │   │   ├── JwtProperties.java          # JWT configuration
│   │   │   │   ├── RateLimitProperties.java    # Rate limit config
│   │   │   │   └── RedisConfig.java            # Redis connection
│   │   │   ├── service/
│   │   │   │   ├── JwtUtil.java                # JWT validation
│   │   │   │   └── RateLimiterService.java     # Rate limiting logic
│   │   │   ├── filter/
│   │   │   │   ├── RequestIdFilter.java        # Request ID generation
│   │   │   │   ├── AuthenticationFilter.java   # JWT authentication
│   │   │   │   ├── RateLimitingFilter.java     # Rate limiting
│   │   │   │   └── LoggingFilter.java          # Request/response logging
│   │   │   ├── exception/
│   │   │   │   └── GlobalExceptionHandler.java # Error handling
│   │   │   └── controller/
│   │   │       └── FallbackController.java     # Circuit breaker fallbacks
│   │   └── resources/
│   │       └── application.yml                  # Configuration
│   └── test/
│       └── java/com/ecommerce/gateway/
│           └── ApiGatewayApplicationTests.java
├── pom.xml
└── README.md
```

---

## 🛠️ Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Programming Language |
| Spring Boot | 3.5.7 | Application Framework |
| Spring Cloud Gateway | 2025.0.0 | API Gateway |
| Spring WebFlux | 6.x | Reactive Web Framework |
| JJWT | 0.12.5 | JWT Parsing & Validation |
| Nimbus JOSE JWT | 9.37.3 | JWKS Parsing |
| Bucket4j | 8.7.0 | Rate Limiting (Token Bucket) |
| Redis | 7.x | Distributed Rate Limit Storage |
| Lettuce | Latest | Redis Client |
| Project Reactor | 3.6.x | Reactive Programming |
| Lombok | 1.18.x | Boilerplate Reduction |
| Maven | 3.9+ | Build Tool |

---

## Additional Documentation

- **[API_GATEWAY_SECURITY_CONFIG.md](../API_GATEWAY_SECURITY_CONFIG.md)** - Detailed security configuration guide
- **[API_GATEWAY_TESTING_GUIDE.md](../API_GATEWAY_TESTING_GUIDE.md)** - Comprehensive testing scenarios
- **[API_GATEWAY_COMPLETE.md](../API_GATEWAY_COMPLETE.md)** - Implementation summary

---

## Summary

### Features:
- JWT Authentication (offline with JWKS)
- Token Bucket Rate Limiting (per user/IP)
- Request ID Correlation
- Request/Response Logging
- Error Handling
- Circuit Breaker Support

### Routes:
- Order Service - 50 req/min per user
- Analytics Service - 100 req/min per user
- Notification Service - 30 req/min per user
- Health Check - 10 req/min per IP (no auth)

### Requirements:
- Redis (port 6379)
- Authorization Server (port 9000)
- Backend Services (8080, 8081, 8082)

---

**Version:** 1.0-SNAPSHOT  
**Port:** 8090  
**Status:** Production Ready
