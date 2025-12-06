package org.lampis.order.service;

import org.lampis.common.event.order.OrderCancelledEvent;
import org.lampis.common.event.order.OrderCreatedEvent;
import org.lampis.common.event.order.OrderStatusChangedEvent;
import org.lampis.common.event.order.OrderUpdatedEvent;

/**
 * Service for publishing order events to RabbitMQ
 */
public interface EventPublisherService {

    /**
     * Publish order created event
     */
    void publishOrderCreatedEvent(OrderCreatedEvent event);

    /**
     * Publish order updated event
     */
    void publishOrderUpdatedEvent(OrderUpdatedEvent event);

    /**
     * Publish order status changed event
     */
    void publishOrderStatusChangedEvent(OrderStatusChangedEvent event);

    /**
     * Publish order cancelled event
     */
    void publishOrderCancelledEvent(OrderCancelledEvent event);
}
