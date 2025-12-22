# Authorization Server

Production-ready OAuth 2.0 Authorization Server with OpenID Connect (OIDC) support, providing secure authentication and JWT token generation for the eCommerce Order Management System.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [OAuth2 Clients](#oauth2-clients)
- [Users & Credentials](#users--credentials)
- [Testing](#testing)
- [Integration](#integration)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)

---

## Overview

The Authorization Server is the authentication hub for the entire microservices ecosystem. It issues secure JWT tokens that are validated by the API Gateway, enabling stateless, scalable authentication across all services.

### Key Responsibilities:
- **User Authentication** - Validates user credentials and issues tokens
- **Client Authentication** - Authenticates applications requesting access
- **Token Issuance** - Generates JWT access tokens with RSA signing
- **Key Distribution** - Provides public keys via JWKS endpoint
- **OAuth2 & OIDC** - Full protocol compliance for industry standards

---

## Features

### OAuth 2.0 & OpenID Connect
- **OAuth 2.0 Authorization Server** - Complete protocol implementation
- **OpenID Connect 1.0** - Identity layer for user authentication
- **JWT Tokens** - RS256 algorithm with 2048-bit RSA keys
- **JWKS Endpoint** - Public key distribution for offline validation
- **Token Introspection** - Check token validity and metadata
- **Token Revocation** - Invalidate tokens before expiration

### Grant Types Supported
- **Client Credentials** - Machine-to-machine authentication
- **Authorization Code** - Secure user authentication via browser
- **Authorization Code + PKCE** - Enhanced security for mobile/SPA
- **Refresh Token** - Long-lived sessions without re-authentication

### Security Features
- **RSA Signing** - 2048-bit keys for JWT signatures
- **BCrypt Password Hashing** - Secure password storage (strength 10)
- **PKCE Support** - Proof Key for Code Exchange for public clients
- **Scope-Based Authorization** - Fine-grained permission control
- **Token Customization** - Additional claims in JWT payload

### Integration Ready
- **API Gateway Compatible** - JWKS endpoint for offline validation
- **Microservices Ready** - Standard OAuth2/OIDC protocols
- **Spring Security Native** - Seamless Spring ecosystem integration

---

## Architecture

### System Context

```
┌─────────────────────────────────────┐
│        Clients & Applications       │
│   (Web, Mobile, Services)           │
└───────────────┬─────────────────────┘
                │
                │ 1. Request Token
                │    POST /oauth2/token
                ↓
┌──────────────────────────────────────────┐
│   Authorization Server (Port 9000)       │
│                                          │
│  ┌────────────────────────────────────┐ │
│  │  Authentication                    │ │
│  │  - Validate credentials            │ │
│  │  - Verify client_id/secret         │ │
│  │  - Check grant type support        │ │
│  └────────────────────────────────────┘ │
│                ↓                         │
│  ┌────────────────────────────────────┐ │
│  │  Authorization                     │ │
│  │  - Validate requested scopes       │ │
│  │  - Check user permissions          │ │
│  │  - Create authorization grant      │ │
│  └────────────────────────────────────┘ │
│                ↓                         │
│  ┌────────────────────────────────────┐ │
│  │  Token Generation                  │ │
│  │  - Build JWT with claims           │ │
│  │  - Sign with RSA private key       │ │
│  │  - Add custom claims (user info)   │ │
│  └────────────────────────────────────┘ │
│                ↓                         │
│  2. Return JWT Token                    │
└──────────────┬───────────────────────────┘
               │
               │ 3. Use Token
               ↓
┌──────────────────────────────────────────┐
│        API Gateway (Port 8090)           │
│                                          │
│  ┌────────────────────────────────────┐ │
│  │  Token Validation (OFFLINE)        │ │
│  │  - Fetch public key from JWKS      │ │
│  │  - Verify signature                │ │
│  │  - Check expiration                │ │
│  │  - Extract user claims             │ │
│  └────────────────────────────────────┘ │
│                ↓                         │
│  4. Forward to Microservices            │
│     (with user context headers)         │
└─────────────────────────────────────────┘
```

---

## Prerequisites

- **Java 21+** - Programming language runtime
- **Maven 3.9+** - Build tool
- **No External Dependencies** - Uses in-memory storage for demo

---

## Installation

### 1. Navigate to Module

```bash
cd ecommerce-order-management-system/authorization-server
```

### 2. Build the Project

```bash
mvn clean install
```

### 3. Run the Application

```bash
mvn spring-boot:run
```

The authorization server will start on **port 9000**.

### 4. Verify It's Running

```bash
# Health check
curl http://localhost:9000/actuator/health

# OAuth2 metadata
curl http://localhost:9000/.well-known/oauth-authorization-server

# JWKS (public keys for token validation)
curl http://localhost:9000/oauth2/jwks
```

---

## Configuration

### Main Configuration File

**Location:** `src/main/resources/application.yml`

```yaml
server:
  port: 9000

spring:
  application:
    name: authorization-server
  security:
    oauth2:
      authorizationserver:
        issuer: http://localhost:9000

logging:
  level:
    com.ecommerce.authserver: DEBUG
```

### Configuration Classes

| Class | Purpose |
|-------|---------|
| **SecurityConfig** | OAuth2 security filter chains |
| **ClientConfig** | Registered OAuth2 clients |
| **UserConfig** | User authentication |
| **JwkConfig** | RSA key pair for JWT signing |
| **TokenCustomizerConfig** | Custom JWT claims |

---

## OAuth2 Clients

### 1. API Gateway Client (Client Credentials)

```yaml
Client ID: ecommerce-api-gateway
Client Secret: gateway-secret-2024
Grant Types: client_credentials
Scopes: api.read, api.write, service.access
Token Lifetime: 1 hour
```

**Example:**
```bash
curl -X POST http://localhost:9000/oauth2/token \
  -u ecommerce-api-gateway:gateway-secret-2024 \
  -d "grant_type=client_credentials"
```

### 2. Web Application Client (Authorization Code)

```yaml
Client ID: ecommerce-web-app
Client Secret: web-secret-2024
Grant Types: authorization_code, refresh_token
Redirect URI: http://localhost:3000/callback
Scopes: openid, profile, email, orders.read, orders.write
```

### 3. Mobile App Client (Authorization Code + PKCE)

```yaml
Client ID: ecommerce-mobile-app
Grant Types: authorization_code, refresh_token
PKCE: Required
Redirect URI: com.ecommerce.mobile://callback
```

### 4. Admin Portal Client (Authorization Code)

```yaml
Client ID: ecommerce-admin
Client Secret: admin-secret-2024
Grant Types: authorization_code, refresh_token
Redirect URI: http://localhost:8080/admin/callback
Scopes: admin.read, admin.write, user.manage
```

---

## Users & Credentials

### 1. Admin User
```
Email: admin@ecommerce.com
Password: admin123
Roles: ADMIN, USER
```

### 2. Regular User - John
```
Email: john@ecommerce.com
Password: john123
Roles: USER
```

### 3. Regular User - Jane
```
Email: jane@ecommerce.com
Password: jane123
Roles: USER
```

---

## Testing

### Get Token (Client Credentials)

```bash
curl -X POST http://localhost:9000/oauth2/token \
  -u ecommerce-api-gateway:gateway-secret-2024 \
  -d "grant_type=client_credentials"
```

### Verify JWKS Endpoint

```bash
curl http://localhost:9000/oauth2/jwks | jq
```

### Decode JWT Token

```bash
TOKEN=$(curl -s -X POST http://localhost:9000/oauth2/token \
  -u ecommerce-api-gateway:gateway-secret-2024 \
  -d "grant_type=client_credentials" \
  | jq -r '.access_token')

echo $TOKEN | cut -d '.' -f 2 | base64 -d | jq
```

---

## Integration

### With API Gateway

```yaml
# API Gateway application.yml
jwt:
  jwks-uri: http://localhost:9000/oauth2/jwks
  issuer: http://localhost:9000
```

**How It Works:**
1. API Gateway fetches JWKS on startup
2. Validates tokens **offline** using public key
3. No network call to auth server per request
4. Fast validation (< 10ms)

### With Microservices

Headers forwarded by API Gateway:
```
X-User-Id: john
X-User-Role: ROLE_USER
X-User-Email: john@ecommerce.com
X-Authenticated: true
```

---

## Monitoring

### Health Check

```bash
curl http://localhost:9000/actuator/health
```

### OAuth2 Discovery

```bash
curl http://localhost:9000/.well-known/oauth-authorization-server | jq
```

---

## Troubleshooting

### Issue: "Invalid client credentials"
- Verify client ID and secret
- Check ClientConfig.java

### Issue: "Unsupported grant type"
- Client must be configured with requested grant type

### Issue: JWT signature verification fails
- Check JWKS endpoint accessibility
- Verify issuer matches between auth server and API gateway

---

## Project Structure

```
authorization-server/
├── src/
│   ├── main/
│   │   ├── java/com/ecommerce/authserver/
│   │   │   ├── AuthorizationServerApplication.java
│   │   │   └── config/
│   │   │       ├── SecurityConfig.java
│   │   │       ├── ClientConfig.java
│   │   │       ├── UserConfig.java
│   │   │       ├── JwkConfig.java
│   │   │       └── TokenCustomizerConfig.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
├── pom.xml
└── README.md
```

---

## Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Programming Language |
| Spring Boot | 3.5.7 | Application Framework |
| Spring Security | 6.x | Security Framework |
| Spring Authorization Server | 1.2.0+ | OAuth2/OIDC Server |
| Nimbus JOSE JWT | 9.37.3 | JWT & JWKS Support |
| BCrypt | Latest | Password Hashing |

---

## Key Endpoints

| Endpoint | Purpose |
|----------|---------|
| `/oauth2/token` | Token endpoint |
| `/oauth2/authorize` | Authorization endpoint |
| `/oauth2/jwks` | Public keys (JWKS) |
| `/oauth2/introspect` | Token introspection |
| `/oauth2/revoke` | Token revocation |
| `/userinfo` | UserInfo (OIDC) |
| `/.well-known/oauth-authorization-server` | OAuth2 metadata |
| `/actuator/health` | Health check |

---

## Summary

### Features:
- OAuth 2.0 Authorization Server
- OpenID Connect (OIDC)
- JWT tokens (RS256)
- JWKS endpoint
- 4 OAuth2 clients
- 3 demo users

### Clients:
1. ecommerce-api-gateway (client_credentials)
2. ecommerce-web-app (authorization_code)
3. ecommerce-mobile-app (authorization_code + PKCE)
4. ecommerce-admin (authorization_code)

---

**Version:** 1.0-SNAPSHOT  
**Port:** 9000  
**Status:** Production Ready
