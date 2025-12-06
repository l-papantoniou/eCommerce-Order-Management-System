package org.lampis.common.event.order;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.lampis.common.enums.EventType;
import org.lampis.common.event.BaseEvent;

import java.math.BigDecimal;

/**
 * Event published when an order is updated
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderUpdatedEvent extends BaseEvent {

    private Long orderId;
    private Long customerId;
    private BigDecimal totalAmount;

    public OrderUpdatedEvent(Long orderId, Long customerId, BigDecimal totalAmount) {
        super();
        this.orderId = orderId;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        setEventType(EventType.ORDER_UPDATED);
    }
}
