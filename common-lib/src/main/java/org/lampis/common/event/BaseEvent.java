package org.lampis.common.event;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.lampis.common.enums.EventType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all domain events
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = org.lampis.common.event.order.OrderCreatedEvent.class, name = "ORDER_CREATED"),
        @JsonSubTypes.Type(value = org.lampis.common.event.order.OrderUpdatedEvent.class, name = "ORDER_UPDATED"),
        @JsonSubTypes.Type(value = org.lampis.common.event.order.OrderStatusChangedEvent.class, name = "ORDER_STATUS_CHANGED"),
        @JsonSubTypes.Type(value = org.lampis.common.event.order.OrderCancelledEvent.class, name = "ORDER_CANCELLED")
})
public abstract class BaseEvent implements Serializable {

    private String eventId;
    private EventType eventType;
    private LocalDateTime timestamp;
    private String correlationId;

    /**
     * Initialize event metadata
     */
    public void initializeMetadata(String correlationId) {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.correlationId = correlationId;
    }
}
