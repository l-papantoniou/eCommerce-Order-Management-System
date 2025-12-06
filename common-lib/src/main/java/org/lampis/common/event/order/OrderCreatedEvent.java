package org.lampis.common.event.order;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.lampis.common.enums.EventType;
import org.lampis.common.enums.OrderStatus;
import org.lampis.common.event.BaseEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event published when a new order is created
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderCreatedEvent extends BaseEvent {

    private Long orderId;
    private Long customerId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime orderDate;

    public OrderCreatedEvent(Long orderId, Long customerId, OrderStatus status, BigDecimal totalAmount, LocalDateTime orderDate) {
        super();
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.orderDate = orderDate;
        setEventType(EventType.ORDER_CREATED);
    }
}
