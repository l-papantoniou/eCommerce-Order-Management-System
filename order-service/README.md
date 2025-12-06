# Order Service

Core microservice for managing customer orders in the eCommerce platform.

## Features

- **Order Management** - Create, read, update, delete orders
- **Inventory Validation** - Automatic stock checking and reservation
- **Status Lifecycle** - Automatic progression: UNPROCESSED → PROCESSING → PROCESSED → SHIPPED
- **Audit Trail** - Track all order modifications
- **Event Publishing** - RabbitMQ events for order changes
- **Soft Delete** - Orders are never permanently deleted

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/orders` | Create new order |
| GET | `/api/v1/orders/{id}` | Get order by ID |
| GET | `/api/v1/orders` | List all orders (with filters) |
| PUT | `/api/v1/orders/{id}` | Update order |
| PATCH | `/api/v1/orders/{id}/status` | Update order status |
| DELETE | `/api/v1/orders/{id}` | Soft delete order |
| GET | `/api/v1/orders/{id}/history` | Get audit trail |

## Database Schema

### Tables

**orders**
- id, customer_id, status, order_date, total_amount
- deleted, created_at, updated_at

**order_lines**
- id, order_id, product_id, quantity, unit_price, line_total

**inventory**
- id, product_id, product_name, available_stock

**order_audit**
- id, order_id, field_name, old_value, new_value, changed_at, changed_by

## Configuration

```yaml
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/orderdb

# RabbitMQ
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672

# Server
server.port=8080

# Status progression (milliseconds)
order.status.progression.interval=300000
```

## Running

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Swagger UI
http://localhost:8080/swagger-ui.html
```

## Dependencies

- Spring Boot Web
- Spring Data JPA
- PostgreSQL
- RabbitMQ (AMQP)
- SpringDoc OpenAPI
- common-lib

## Order Lifecycle

```
UNPROCESSED → PROCESSING → PROCESSED → SHIPPED
     ↓            ↓           ↓
  CANCELLED    CANCELLED   CANCELLED
```

## Events Published

- `OrderCreatedEvent` - When order is created
- `OrderUpdatedEvent` - When order is modified
- `OrderStatusChangedEvent` - When status changes

---

**Version**: 1.0-SNAPSHOT  
**Port**: 8080