package org.lampis.order.service.impl;

import org.lampis.common.config.RabbitMQConfig;
import org.lampis.common.event.order.OrderCancelledEvent;
import org.lampis.common.event.order.OrderCreatedEvent;
import org.lampis.common.event.order.OrderStatusChangedEvent;
import org.lampis.common.event.order.OrderUpdatedEvent;
import org.lampis.order.service.EventPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Implementation of EventPublisherService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisherServiceImpl implements EventPublisherService {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent for order: {}", event.getOrderId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                event
        );
    }

    @Override
    public void publishOrderUpdatedEvent(OrderUpdatedEvent event) {
        log.info("Publishing OrderUpdatedEvent for order: {}", event.getOrderId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_UPDATED_ROUTING_KEY,
                event
        );
    }

    @Override
    public void publishOrderStatusChangedEvent(OrderStatusChangedEvent event) {
        log.info("Publishing OrderStatusChangedEvent for order: {} from {} to {}",
                event.getOrderId(), event.getOldStatus(), event.getNewStatus());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_STATUS_CHANGED_ROUTING_KEY,
                event
        );
    }

    @Override
    public void publishOrderCancelledEvent(OrderCancelledEvent event) {
        log.info("Publishing OrderCancelledEvent for order: {}", event.getOrderId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CANCELLED_ROUTING_KEY,
                event
        );
    }
}