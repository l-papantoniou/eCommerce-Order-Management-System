# Notification Service

Async notification service that consumes order events and sends multi-channel notifications with retry logic.

## Features

- **Event-Driven** - Listens to RabbitMQ order events
- **Multi-Channel** - Email and SMS notifications
- **Retry Logic** - Exponential backoff (3 attempts)
- **Dead Letter Queue** - Failed message handling
- **Notification Tracking** - In-memory log of all attempts

## Event Listeners

| Queue | Event | Notification |
|-------|-------|--------------|
| `order.created.queue` | OrderCreatedEvent | Email + SMS confirmation |
| `order.status.changed.queue` | OrderStatusChangedEvent | SMS status update |
| `dlq.queue` | Failed messages | Logging + alerting |

## Retry Mechanism

**RabbitMQ Listener Retry:**
- Max attempts: 3
- Initial delay: 1 second
- Multiplier: 2x (exponential backoff)
- Max delay: 10 seconds

**Retry Flow:**
```
Attempt 1: Process message
  ↓ (fail)
Wait 1 second
  ↓
Attempt 2: Retry processing
  ↓ (fail)
Wait 2 seconds
  ↓
Attempt 3: Final retry
  ↓ (fail)
→ Send to Dead Letter Queue
```

**Total: 3 attempts per message**

## Notification Types

### Email
- Order confirmations
- Success rate: 80% (simulated)
- Includes subject + message

### SMS
- Order confirmations
- Status updates
- Success rate: 85% (simulated)

## Configuration

```yaml
notification:
  max-attempts: 3
  email:
    enabled: true
  sms:
    enabled: true

rabbitmq:
  host: localhost
  port: 5672
  listener:
    simple:
      retry:
        enabled: true
        max-attempts: 3
```

## Running

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Runs on port 8081
```

## Example Flow

```
Order Service → OrderCreatedEvent
      ↓
RabbitMQ Queue (order.created.queue)
      ↓
Notification Service
      ↓
[Email Service] → customer@example.com ✓
[SMS Service] → +1234567890 ✓
      ↓
Notification Logs (in-memory)
```

## Retry Example

```
Attempt 1: ✗ Failed
  ↓ (wait 1s)
Attempt 2: ✗ Failed
  ↓ (wait 2s)
Attempt 3: ✓ Success
  ↓
Status: SENT
```

## Dependencies

- Spring AMQP (RabbitMQ)
- Spring Retry
- common-lib

---

**Version**: 1.0-SNAPSHOT  
**Port**: 8081