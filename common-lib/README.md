# Common-Lib

Shared library for the eCommerce Order Management System microservices.

## Overview

The **common-lib** module provides reusable components across all microservices. It contains DTOs, domain events, exceptions, and configuration constants.

## What's Inside

- **DTOs** - Request/response objects with validation
- **Events** - Domain events for RabbitMQ messaging
- **Enums** - OrderStatus, EventType, NotificationType
- **Exceptions** - Custom business exceptions
- **Config** - RabbitMQ queue and exchange names

## Components

### 1. DTOs (Data Transfer Objects)

Request and response objects with built-in validation.

```java
// Create order request
CreateOrderRequest request = CreateOrderRequest.builder()
    .customerId(123L)
    .orderLines(List.of(
        OrderLineDTO.builder()
            .productId(1L)
            .quantity(2)
            .unitPrice(new BigDecimal("29.99"))
            .build()
    ))
    .build();
```

**Available:**
- `CreateOrderRequest`, `UpdateOrderRequest`, `UpdateOrderStatusRequest`
- `OrderResponse`, `OrderLineDTO`, `OrderAuditResponse`
- `ErrorResponse` - Standard error format

### 2. Events

Domain events for RabbitMQ messaging.

```java
OrderCreatedEvent event = new OrderCreatedEvent(
    orderId,
    customerId,
    OrderStatus.UNPROCESSED,
    totalAmount,
    LocalDateTime.now()
);

rabbitTemplate.convertAndSend(
    RabbitMQConfig.ORDER_EXCHANGE,
    RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
    event
);
```

**Available:**
- `OrderCreatedEvent`, `OrderUpdatedEvent`
- `OrderStatusChangedEvent`, `OrderCancelledEvent`

### 3. OrderStatus Enum

State machine for order lifecycle.

```java
OrderStatus status = OrderStatus.UNPROCESSED;
OrderStatus next = status.getNextStatus(); // PROCESSING

// Validate transitions
if (status.canTransitionTo(OrderStatus.PROCESSED)) {
    // Valid transition
}
```

**Lifecycle:**
```
UNPROCESSED → PROCESSING → PROCESSED → SHIPPED
     ↓            ↓           ↓
  CANCELLED    CANCELLED   CANCELLED
```

### 4. Exceptions

```java
throw new ResourceNotFoundException("Order", "id", orderId);
throw new InsufficientStockException(productId, requestedQty, availableQty);
throw new InvalidOrderStateException(currentStatus, targetStatus);
```

### 5. RabbitMQ Config

Constants for queues, exchanges, and routing keys.

```java
RabbitMQConfig.ORDER_EXCHANGE
RabbitMQConfig.ORDER_CREATED_QUEUE
RabbitMQConfig.ORDER_CREATED_ROUTING_KEY
```

## Usage

Add to your service's `pom.xml`:

```xml
<dependency>
    <groupId>com.ecommerce</groupId>
    <artifactId>common-lib</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### Example: Create and Publish Order Event

```java
@Service
public class OrderService {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public OrderResponse createOrder(CreateOrderRequest request) {
        // Create order entity
        Order order = // ... save to database
        
        // Publish event
        OrderCreatedEvent event = new OrderCreatedEvent(
            order.getId(),
            order.getCustomerId(),
            order.getStatus(),
            order.getTotalAmount(),
            order.getOrderDate()
        );
        
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.ORDER_EXCHANGE,
            RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
            event
        );
        
        return toDTO(order);
    }
    
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = findById(orderId);
        
        // Validate state transition
        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw new InvalidOrderStateException(order.getStatus(), newStatus);
        }
        
        order.setStatus(newStatus);
        // ... save to database
    }
}
```

### Example: Consume Events

```java
@Component
public class OrderEventListener {
    
    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Order created: {}", event.getOrderId());
        // Send notifications
    }
    
    @RabbitListener(queues = RabbitMQConfig.ORDER_STATUS_CHANGED_QUEUE)
    public void handleStatusChanged(OrderStatusChangedEvent event) {
        log.info("Order {} changed from {} to {}", 
            event.getOrderId(), 
            event.getOldStatus(), 
            event.getNewStatus());
        // Update analytics
    }
}
```

## Building

```bash
# Build and install
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Run tests
mvn test
```

## Project Structure

```
common-lib/
└── src/main/java/com/ecommerce/common/
    ├── config/              # RabbitMQ constants
    ├── dto/                 # Data Transfer Objects
    │   ├── error/
    │   └── order/
    ├── enums/              # OrderStatus, EventType, NotificationType
    ├── event/              # Domain events
    │   └── order/
    ├── exception/          # Custom exceptions
    └── util/               # Utilities (DateTimeUtil, CorrelationIdHolder)
```

## Key Features

- **Validation** - DTOs include `@NotNull`, `@Min`, `@NotEmpty` annotations
- **Immutability** - Lombok builders for clean, immutable DTOs
- **State Machine** - OrderStatus with transition validation
- **Event Support** - Ready for RabbitMQ event-driven architecture

---

**Version**: 1.0-SNAPSHOT  
**Java**: 21  
**Spring Boot**: 3.2.0