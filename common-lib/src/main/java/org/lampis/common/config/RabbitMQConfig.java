package org.lampis.common.config;

/**
 * RabbitMQ configuration constants shared across services
 */
public final class RabbitMQConfig {

    private RabbitMQConfig() {
        // Constants class
    }

    // Exchange names
    public static final String ORDER_EXCHANGE = "order.exchange";

    // Queue names
    public static final String ORDER_CREATED_QUEUE = "order.created.queue";
    public static final String ORDER_UPDATED_QUEUE = "order.updated.queue";
    public static final String ORDER_STATUS_CHANGED_QUEUE = "order.status.changed.queue";
    public static final String ORDER_CANCELLED_QUEUE = "order.cancelled.queue";

    // Routing keys
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String ORDER_UPDATED_ROUTING_KEY = "order.updated";
    public static final String ORDER_STATUS_CHANGED_ROUTING_KEY = "order.status.changed";
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.cancelled";

    // Dead Letter Queue
    public static final String DLQ_EXCHANGE = "dlq.exchange";
    public static final String DLQ_QUEUE = "dlq.queue";
    public static final String DLQ_ROUTING_KEY = "dlq";
}
