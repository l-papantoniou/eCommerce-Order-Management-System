package org.lampis.notification.listener;

import org.lampis.common.config.RabbitMQConfig;
import org.lampis.common.enums.NotificationType;
import org.lampis.common.event.order.OrderCreatedEvent;
import org.lampis.common.event.order.OrderStatusChangedEvent;
import org.lampis.notification.model.NotificationLog;
import org.lampis.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener for order events from RabbitMQ
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final NotificationService notificationService;

    /**
     * Handle order created event
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for order: {}", event.getOrderId());

        try {
            // Create email notification
            NotificationLog emailNotification = notificationService.createOrderCreatedNotification(
                    event.getOrderId(),
                    event.getCustomerId()
            );

            // Send with retry
            notificationService.sendNotification(emailNotification);

            // Create SMS notification
            NotificationLog smsNotification = NotificationLog.builder()
                    .id(java.util.UUID.randomUUID().toString())
                    .orderId(event.getOrderId())
                    .customerId(event.getCustomerId())
                    .type(NotificationType.SMS)
                    .recipient("+1234567890" + event.getCustomerId())
                    .message(String.format("Order #%d confirmed! Total: $%.2f",
                            event.getOrderId(),
                            event.getTotalAmount()))
                    .status(NotificationLog.NotificationStatus.PENDING)
                    .attemptCount(0)
                    .createdAt(java.time.LocalDateTime.now())
                    .build();

            notificationService.sendNotification(smsNotification);

            log.info("Notifications processed for order: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("Error processing OrderCreatedEvent for order: {}", event.getOrderId(), e);
            // In production, this would go to DLQ automatically
        }
    }

    /**
     * Handle order status changed event
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_STATUS_CHANGED_QUEUE)
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Received OrderStatusChangedEvent for order: {} ({} → {})",
                event.getOrderId(),
                event.getOldStatus(),
                event.getNewStatus());

        try {
            // Send status update notification
            NotificationLog notification = notificationService.createStatusChangeNotification(
                    event.getOrderId(),
                    event.getCustomerId(),
                    event.getOldStatus().name(),
                    event.getNewStatus().name()
            );

            notificationService.sendNotification(notification);

            log.info("Status change notification sent for order: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("Error processing OrderStatusChangedEvent for order: {}", event.getOrderId(), e);
            // In production, this would go to DLQ automatically
        }
    }

    /**
     * Handle messages from Dead Letter Queue
     */
    @RabbitListener(queues = RabbitMQConfig.DLQ_QUEUE)
    public void handleDeadLetterQueue(org.springframework.amqp.core.Message message) {
        log.error("Message received in DLQ: {}", new String(message.getBody()));
        log.error("Message headers: {}", message.getMessageProperties().getHeaders());

        // In production, you would:
        // 1. Log to monitoring system
        // 2. Store in database for manual review
        // 3. Send alert to operations team
        // 4. Optionally retry with different strategy
    }
}