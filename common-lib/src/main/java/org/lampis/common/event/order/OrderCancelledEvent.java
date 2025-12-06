package org.lampis.common.event.order;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.lampis.common.enums.EventType;
import org.lampis.common.event.BaseEvent;

/**
 * Event published when an order is cancelled
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderCancelledEvent extends BaseEvent {

    private Long orderId;
    private Long customerId;
    private String reason;

    public OrderCancelledEvent(Long orderId, Long customerId, String reason) {
        super();
        this.orderId = orderId;
        this.customerId = customerId;
        this.reason = reason;
        setEventType(EventType.ORDER_CANCELLED);
    }
}
