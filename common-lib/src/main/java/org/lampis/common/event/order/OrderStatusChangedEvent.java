package org.lampis.common.event.order;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.lampis.common.enums.EventType;
import org.lampis.common.enums.OrderStatus;
import org.lampis.common.event.BaseEvent;

/**
 * Event published when order status changes
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderStatusChangedEvent extends BaseEvent {

    private Long orderId;
    private Long customerId;
    private OrderStatus oldStatus;
    private OrderStatus newStatus;

    public OrderStatusChangedEvent(Long orderId, Long customerId, OrderStatus oldStatus, OrderStatus newStatus) {
        super();
        this.orderId = orderId;
        this.customerId = customerId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        setEventType(EventType.ORDER_STATUS_CHANGED);
    }
}
