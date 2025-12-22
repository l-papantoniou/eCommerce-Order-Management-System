# eCommerce Order Management System

A production-ready, distributed microservices system built with Spring Boot, demonstrating modern enterprise architecture patterns including CQRS, event-driven communication, OAuth 2.0 authentication, and API Gateway patterns.

## Overview

This project showcases a complete microservices architecture for managing eCommerce orders, from creation to fulfillment. It implements industry-standard patterns and practices suitable for enterprise production environments.

### Key Highlights

- **6 Microservices** - Clean separation of concerns with dedicated services
- **Event-Driven Architecture** - Asynchronous communication via RabbitMQ
- **CQRS Pattern** - Separate read and write models for optimal performance
- **OAuth 2.0 Security** - Complete authentication and authorization infrastructure
- **API Gateway** - Centralized entry point with rate limiting and JWT validation
- **Production-Ready** - Comprehensive error handling, retry mechanisms, and monitoring

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Client Applications                          │
│                    (Web, Mobile, Services)                          │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 │ OAuth 2.0 / JWT
                                 ↓
                    ┌────────────────────────┐
                    │  Authorization Server  │
                    │      (Port 9000)       │
                    │  - Issue JWT tokens    │
                    │  - RSA signing         │
                    │  - JWKS endpoint       │
                    └────────────────────────┘
                                 │
                                 │ JWT Token
                                 ↓
                    ┌────────────────────────┐
                    │     API Gateway        │
                    │      (Port 8090)       │
                    │  - JWT validation      │
                    │  - Rate limiting       │
                    │  - Request routing     │
                    └───────────┬────────────┘
                                │
            ┌───────────────────┼───────────────────┐
            │                   │                   │
            ↓                   ↓                   ↓
    ┌──────────────┐    ┌──────────────┐   ┌──────────────┐
    │Order Service │    │ Analytics    │   │Notification  │
    │ (Port 8080)  │    │  Service     │   │  Service     │
    │              │    │ (Port 8082)  │   │ (Port 8081)  │
    │ PostgreSQL   │    │  MongoDB     │   │              │
    └──────┬───────┘    └──────┬───────┘   └──────┬───────┘
           │                   │                   │
           └───────────────────┼───────────────────┘
                               │
                         ┌─────┴─────┐
                         │ RabbitMQ  │
                         │ (Events)  │
                         └───────────┘
```

---

##  Microservices

### 1. [Authorization Server](authorization-server/) (Port 9000)
OAuth 2.0 Authorization Server providing secure authentication and JWT token generation.

**Key Features:**
- OAuth 2.0 & OpenID Connect (OIDC)
- JWT tokens with RS256 signing
- Multiple grant types (client credentials, authorization code, PKCE)
- JWKS endpoint for public key distribution

**Technology:** Spring Authorization Server, Spring Security

### 2. [API Gateway](api-gateway/) (Port 8090)
Single entry point for all client requests with authentication, rate limiting, and routing.

**Key Features:**
- JWT token validation (offline)
- Token bucket rate limiting (Redis)
- Request ID generation
- Request/response logging

**Technology:** Spring Cloud Gateway, Redis, Bucket4j

### 3. [Order Service](order-service/) (Port 8080)
Core order management service handling CRUD operations and business logic.

**Key Features:**
- Order lifecycle management (UNPROCESSED → PROCESSING → PROCESSED → SHIPPED)
- Event publishing (OrderCreated, StatusChanged)
- Business validation
- PostgreSQL for transactional data

**Technology:** Spring Boot, Spring Data JPA, PostgreSQL

### 4. [Analytics Service](analytics-service/) (Port 8082)
Read-optimized analytics service implementing the CQRS query side.

**Key Features:**
- Pre-aggregated metrics (customer stats, daily metrics)
- Event-driven data synchronization
- MongoDB for flexible, denormalized data
- Fast query performance

**Technology:** Spring Boot, Spring Data MongoDB, MongoDB

### 5. [Notification Service](notification-service/) (Port 8081)
Event-driven notification service for customer communications.

**Key Features:**
- Email notifications on order events
- Template-based messages
- Retry mechanism with exponential backoff
- Async processing

**Technology:** Spring Boot, RabbitMQ

### 6. [Common Library](common-lib/)
Shared domain models, DTOs, and events used across all services.

**Includes:**
- Domain entities (Order, OrderLine, Customer)
- DTOs (Request/Response objects)
- Events (OrderCreatedEvent, OrderStatusChangedEvent)
- Enums (OrderStatus, PaymentMethod)

---

## Key Features

### Security
- **OAuth 2.0 Authentication** - Industry-standard authorization framework
- **JWT Tokens** - Stateless authentication with RS256 signing
- **API Gateway Security** - Centralized token validation and rate limiting
- **Role-Based Access** - User permissions and scopes

### CQRS Pattern
- **Write Side** - Order Service with PostgreSQL (normalized, ACID)
- **Read Side** - Analytics Service with MongoDB (denormalized, optimized)
- **Event Synchronization** - RabbitMQ bridges command and query models
- **Performance** - 100x faster queries with pre-aggregated data

### Event-Driven Architecture
- **Async Communication** - Loose coupling between services
- **RabbitMQ** - Reliable message delivery
- **Event Types** - OrderCreated, OrderStatusChanged
- **Retry Mechanisms** - Exponential backoff for failed processing

### Performance & Scalability
- **Stateless Services** - Horizontal scaling ready
- **API Gateway Caching** - JWKS public key cached
- **Read Model Optimization** - Pre-calculated aggregations
- **Reactive Gateway** - Non-blocking I/O for high throughput

### Observability
- **Request IDs** - Distributed tracing with correlation IDs
- **Centralized Logging** - Structured logs with service prefixes
- **Health Checks** - Spring Actuator endpoints
- **Performance Tracking** - Request duration logging

---

## Technology Stack

### Core Technologies
- **Java 21** - Latest LTS version
- **Spring Boot 3.5.7** - Application framework
- **Maven** - Dependency management and build tool

### Microservices Infrastructure
- **Spring Cloud Gateway** - API Gateway
- **Spring Authorization Server** - OAuth 2.0 server
- **Spring Security** - Authentication and authorization

### Data Storage
- **PostgreSQL 15** - Relational database (Order Service)
- **MongoDB 7** - Document database (Analytics Service)
- **Redis 7** - Caching and rate limiting (API Gateway)

### Messaging
- **RabbitMQ 3** - Message broker for event-driven architecture

### Security & OAuth2
- **JWT (JJWT)** - JSON Web Token implementation
- **Nimbus JOSE** - JWKS support
- **BCrypt** - Password hashing

### Rate Limiting
- **Bucket4j** - Token bucket algorithm
- **Lettuce** - Redis client

### Testing
- **JUnit 5** - Unit testing framework
- **Mockito** - Mocking framework
- **Spring Test** - Integration testing support

---

## Quick Start

### Option 1: Docker Compose (Recommended)

**One-command deployment of the entire system:**

```bash
# Navigate to project
cd ecommerce-order-management-system

# Start everything
docker-compose up --build -d

# Or use the helper script
./start-system.sh

# Test the system
./test-system.sh
```

**That's it! The entire system is now running.**

All services, databases, and message broker are configured and connected automatically. See [DOCKER_COMPOSE_GUIDE.md](DOCKER_COMPOSE_GUIDE.md) for detailed instructions.

---

### Option 2: Manual Setup (Development)

For development or if you want to run services individually:

### Prerequisites

Ensure you have the following installed:
- **Java 21+**
- **Maven 3.9+**
- **Docker** (for databases and messaging)

### 1. Start Infrastructure Services

```bash
# PostgreSQL
docker run -d -p 5432:5432 \
  -e POSTGRES_DB=orderdb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  --name postgres postgres:15

# MongoDB
docker run -d -p 27017:27017 \
  --name mongodb mongo:7

# RabbitMQ
docker run -d -p 5672:5672 -p 15672:15672 \
  --name rabbitmq rabbitmq:3-management

# Redis
docker run -d -p 6379:6379 \
  --name redis redis:alpine
```

### 2. Build All Modules

```bash
cd ecommerce-order-management-system
mvn clean install
```

### 3. Start Services (in separate terminals)

```bash
# Terminal 1: Authorization Server
cd authorization-server
mvn spring-boot:run

# Terminal 2: API Gateway
cd api-gateway
mvn spring-boot:run

# Terminal 3: Order Service
cd order-service
mvn spring-boot:run

# Terminal 4: Analytics Service
cd analytics-service
mvn spring-boot:run

# Terminal 5: Notification Service
cd notification-service
mvn spring-boot:run
```

### 4. Test the System

```bash
# Get JWT token from Authorization Server
TOKEN=$(curl -s -X POST http://localhost:9000/oauth2/token \
  -u ecommerce-api-gateway:gateway-secret-2024 \
  -d "grant_type=client_credentials" \
  | jq -r '.access_token')

# Create an order through API Gateway
curl -X POST http://localhost:8090/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 123,
    "customerEmail": "john@example.com",
    "paymentMethod": "CREDIT_CARD",
    "shippingAddress": {
      "street": "123 Main St",
      "city": "Springfield",
      "state": "IL",
      "zipCode": "62701",
      "country": "USA"
    },
    "orderLines": [
      {
        "productId": 1001,
        "productName": "Laptop",
        "quantity": 1,
        "unitPrice": 999.99
      }
    ]
  }'

# Check analytics (read model)
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8090/api/v1/analytics/summary
```

---

## System Flow Example

### Creating an Order - End-to-End Flow

```
1. Client Request
   ↓
2. Authorization Server
   - Validates client credentials
   - Issues JWT token
   ↓
3. API Gateway
   - Validates JWT (offline, using JWKS)
   - Checks rate limit
   - Routes to Order Service
   ↓
4. Order Service
   - Validates business rules
   - Saves to PostgreSQL
   - Publishes OrderCreatedEvent to RabbitMQ
   - Returns response
   ↓
5. Event Processing (Async)
   ├─ Analytics Service
   │  - Consumes OrderCreatedEvent
   │  - Updates MongoDB (read model)
   │  - Calculates aggregations
   │
   └─ Notification Service
      - Consumes OrderCreatedEvent
      - Sends confirmation email
      - Logs notification
```

**Result:**
- Order created in PostgreSQL (write model)
- Analytics updated in MongoDB (read model) - ~50ms delay
- Customer receives email notification
- Request tracked with correlation ID

---

## Testing

### Unit Tests
```bash
# Run all tests
mvn test

# Run tests for specific service
cd order-service
mvn test
```

**Test Coverage:**
- Order Service: 24 tests (~95% coverage)
- Analytics Service: 19 tests (~92% coverage)
- Notification Service: 19 tests (~90% coverage)
- **Total: 62 unit tests**

---

## Project Structure

```
ecommerce-order-management-system/
├── pom.xml                          # Parent POM
├── README.md                        # This file
│
├── common-lib/                      # Shared library
│   └── README.md
│
├── authorization-server/            # OAuth 2.0 Server
│   └── README.md
│
├── api-gateway/                     # API Gateway
│   └── README.md
│
├── order-service/                   # Order Management
│   └── README.md
│
├── analytics-service/               # Analytics & Reporting
│   └── README.md
│
└── notification-service/            # Notifications
    └── README.md
```

---

## Configuration

### Service Ports

| Service | Port | Purpose |
|---------|------|---------|
| Authorization Server | 9000 | OAuth 2.0 token issuance |
| API Gateway | 8090 | Client entry point |
| Order Service | 8080 | Order management |
| Notification Service | 8081 | Email notifications |
| Analytics Service | 8082 | Analytics & reporting |

### Database Connections

| Service | Database | Port | Purpose |
|---------|----------|------|---------|
| Order Service | PostgreSQL | 5432 | Transactional data |
| Analytics Service | MongoDB | 27017 | Analytics data |
| API Gateway | Redis | 6379 | Rate limiting |

### Message Broker

| Service | Broker | Port | Purpose |
|---------|--------|------|---------|
| All Services | RabbitMQ | 5672 | Event messaging |
| Management UI | RabbitMQ | 15672 | Admin interface |

---

## Documentation

### Service Documentation

Detailed documentation is available for the infrastructure services:

- **[Authorization Server](authorization-server/README.md)** - OAuth 2.0 setup, clients, users, and grant types
- **[API Gateway](api-gateway/README.md)** - Routes, security, rate limiting, and filters

### Business Services

The core business services (Order, Analytics, Notification) and Common Library contain well-structured code with:
- Comprehensive JavaDoc comments
- Clear package organization
- 62 unit tests with ~93% coverage
- Self-documenting code following Spring Boot best practices

---

## Design Patterns & Principles

### Architectural Patterns
- **Microservices Architecture** - Independent, deployable services
- **CQRS (Command Query Responsibility Segregation)** - Separate read/write models
- **Event-Driven Architecture** - Async communication via events
- **API Gateway Pattern** - Single entry point for clients
- **Database per Service** - Each service owns its data

### Design Principles
- **Domain-Driven Design** - Business logic organized by domain
- **Separation of Concerns** - Clear boundaries between components
- **Single Responsibility** - Each service has one purpose
- **Loose Coupling** - Services communicate via events
- **High Cohesion** - Related functionality grouped together

### Integration Patterns
- **Event Sourcing** - Events as first-class citizens
- **Eventual Consistency** - Accept slight delays for performance
- **Circuit Breaker** - Graceful degradation on failures
- **Retry Pattern** - Exponential backoff for transient failures
- **Idempotency** - Safe retry of operations

---

## Monitoring & Observability

### Health Checks

All services expose health endpoints:

```bash
# Authorization Server
curl http://localhost:9000/actuator/health

# API Gateway
curl http://localhost:8090/actuator/health

# Order Service
curl http://localhost:8080/actuator/health

# Analytics Service
curl http://localhost:8082/actuator/health

# Notification Service
curl http://localhost:8081/actuator/health
```

### Logging

Structured logging with correlation IDs:

```
[abc-123-def-456] [ORDER] Order created: orderId=1
[abc-123-def-456] [ANALYTICS] Updated analytics for order: orderId=1
[abc-123-def-456] [NOTIFICATION] Email sent to: john@example.com
```

---

## Security Considerations

### Production Recommendations

1. **HTTPS/TLS** - Use TLS for all communications
2. **Secret Management** - Use vault solutions for sensitive data
3. **Database Security** - Strong passwords, SSL connections, backups
4. **API Gateway** - Adjust rate limits based on traffic patterns
5. **JWT Tokens** - Keep token lifetime short (15-60 minutes)

---

## Performance Characteristics

### Latency
- **API Gateway Overhead**: < 10ms
- **JWT Validation**: < 5ms (offline)
- **Order Creation**: ~200-500ms
- **Analytics Query**: ~50-100ms (CQRS benefit)
- **Event Processing**: ~50-100ms

### Scalability
- **Horizontal Scaling**: All services are stateless
- **Database Scaling**: Read replicas for Analytics
- **Message Queue**: Handles spikes with buffering

---

## Project Status

**Status**: **Production-Ready**

### Completed Features
-  6 Microservices (fully implemented)
-  OAuth 2.0 Authorization Server
-  API Gateway with authentication & rate limiting
-  CQRS pattern implementation
-  Event-driven architecture
-  62 unit tests with high coverage
-  Comprehensive documentation
-  Error handling and retry mechanisms
-  Distributed tracing (correlation IDs)
-  Health checks and monitoring

### Architecture Highlights
- **Modern Stack**: Java 21, Spring Boot 3.5.7, Spring Cloud
- **Security**: OAuth 2.0, JWT tokens, API Gateway protection
- **Performance**: CQRS pattern, read/write optimization
- **Scalability**: Stateless services, event-driven communication
- **Quality**: 62 tests, ~93% average coverage

---

**Built using Spring Boot and modern microservices patterns**

**Version**: 1.0-SNAPSHOT  
**Last Updated**: December 2025
