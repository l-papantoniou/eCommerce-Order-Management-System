package org.lampis.analytics.listener;


import org.lampis.analytics.model.OrderAnalytics;
import org.lampis.analytics.service.AnalyticsService;
import org.lampis.common.config.RabbitMQConfig;
import org.lampis.common.event.order.OrderCreatedEvent;
import org.lampis.common.event.order.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Listener for order events to update analytics
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final AnalyticsService analyticsService;

    /**
     * Handle order created event
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for analytics: orderId={}", event.getOrderId());

        try {
            // Convert event items to analytics items
            List<OrderAnalytics.OrderLineItem> items = event.getOrderLines().stream()
                    .map(item -> OrderAnalytics.OrderLineItem.builder()
                            .productId(item.getProductId())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .lineTotal(item.getUnitPrice().multiply(
                                    java.math.BigDecimal.valueOf(item.getQuantity())))
                            .build())
                    .collect(Collectors.toList());

            analyticsService.processOrderCreated(
                    event.getOrderId(),
                    event.getCustomerId(),
                    event.getStatus(),
                    event.getTotalAmount(),
                    event.getOrderDate(),
                    items
            );

            log.info("Order created analytics processed successfully: orderId={}", event.getOrderId());

        } catch (Exception e) {
            log.error("Error processing OrderCreatedEvent for analytics: orderId={}",
                    event.getOrderId(), e);
            // In production, this would go to DLQ
            throw new RuntimeException("Failed to process order created analytics", e);
        }
    }

    /**
     * Handle order status changed event
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_STATUS_CHANGED_QUEUE)
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Received OrderStatusChangedEvent for analytics: orderId={}, {} -> {}",
                event.getOrderId(), event.getOldStatus(), event.getNewStatus());

        try {
            analyticsService.processOrderStatusChanged(
                    event.getOrderId(),
                    event.getCustomerId(),
                    event.getOldStatus(),
                    event.getNewStatus()
            );

            log.info("Order status change analytics processed successfully: orderId={}",
                    event.getOrderId());

        } catch (Exception e) {
            log.error("Error processing OrderStatusChangedEvent for analytics: orderId={}",
                    event.getOrderId(), e);
            // In production, this would go to DLQ
            throw new RuntimeException("Failed to process order status change analytics", e);
        }
    }
}