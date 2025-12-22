# Analytics Service

Event-driven analytics service that aggregates order data in MongoDB and provides reporting REST APIs.

## CQRS Pattern Implementation 

This service implements the **Query Side** of the CQRS (Command Query Responsibility Segregation) pattern.

### Why CQRS?

**Problem with traditional approach:**
- Complex JOIN queries slow down as data grows
- Reporting queries compete with transactional writes
- Same normalized structure for both reads and writes is inefficient

**Our solution:**
- **Command Side (Order Service):** Handles writes, optimized for transactions (PostgreSQL)
- **Query Side (Analytics Service):** Handles reads, optimized for reporting (MongoDB)

### How It Works

```
Order Service (Write)          Analytics Service (Read)
      ↓                               ↑
  PostgreSQL                      MongoDB
 (Normalized)                  (Denormalized)
      ↓                               ↑
      └──── RabbitMQ Events ──────────┘
```

**Flow:**
1. Order created → Saved to PostgreSQL (normalized)
2. Event published → RabbitMQ
3. Analytics listens → Transforms to read model
4. Saved to MongoDB → Pre-aggregated, denormalized

**Benefits:**
- **Fast queries** - No joins, pre-calculated aggregations
- **Scalability** - Read and write databases scale independently
- **Performance** - Reporting doesn't slow down order processing
- **Flexibility** - Different data models optimized for their purpose

**Example:**
```bash
# Query pre-aggregated data (instant response)
GET /api/v1/analytics/customers/top-revenue

# Returns data already calculated in MongoDB
{
  "customerId": 100,
  "totalOrders": 15,      # Pre-calculated
  "totalRevenue": 1499.85 # Pre-calculated
}
```

vs traditional approach requiring expensive JOINs and GROUP BY operations.

---

## Features

- **MongoDB Integration** - 3 collections for different aggregation levels
- **Event Consumers** - Listens to order events from RabbitMQ
- **CQRS Pattern** - Separate read model optimized for reporting
- **REST API** - 9 endpoints for business intelligence
- **Real-time Updates** - Analytics updated as events occur

## MongoDB Collections

### OrderAnalytics
- Order-level denormalized data
- Indexed by orderId, customerId, status, orderDate

### CustomerAnalytics
- Aggregated customer statistics
- Total orders, revenue, average order value
- Order counts by status

### DailyMetrics
- Time-series data for trends
- Daily orders, revenue, averages

## Event Listeners

| Queue | Event | Action |
|-------|-------|--------|
| `order.created.queue` | OrderCreatedEvent | Create analytics records |
| `order.status.changed.queue` | OrderStatusChangedEvent | Update analytics |

## REST API Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /api/v1/analytics/orders/{orderId}` | Order analytics |
| `GET /api/v1/analytics/customers/{customerId}` | Customer analytics |
| `GET /api/v1/analytics/orders/status/{status}` | Orders by status |
| `GET /api/v1/analytics/customers/top-revenue?limit=10` | Top customers |
| `GET /api/v1/analytics/metrics/daily?startDate=...&endDate=...` | Daily metrics range |
| `GET /api/v1/analytics/metrics/recent` | Last 30 days |
| `GET /api/v1/analytics/summary` | Summary statistics |
| `GET /api/v1/analytics/orders/count/{status}` | Count by status |
| `GET /api/v1/analytics/health` | Health check |

## Configuration

```yaml
spring:
  data:
    mongodb:
      host: localhost
      port: 27017
      database: ecommerce_analytics

  rabbitmq:
    host: localhost
    port: 5672

server:
  port: 8082
```

## Running

```bash
# Prerequisites: MongoDB and RabbitMQ running

# Build
mvn clean install

# Run
mvn spring-boot:run

# Runs on port 8082
```

## Example Usage

```bash
# Get summary statistics
curl http://localhost:8082/api/v1/analytics/summary

# Get top customers by revenue
curl http://localhost:8082/api/v1/analytics/customers/top-revenue?limit=5

# Get daily metrics for last week
curl "http://localhost:8082/api/v1/analytics/metrics/daily?startDate=2024-12-01&endDate=2024-12-07"
```

## Dependencies

- Spring Data MongoDB
- Spring AMQP (RabbitMQ)
- Spring Web
- common-lib

---

**Version**: 1.0-SNAPSHOT  
**Port**: 8082
