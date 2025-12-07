package org.lampis.common.event.order;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.lampis.common.dto.order.OrderLineDTO;
import org.lampis.common.enums.EventType;
import org.lampis.common.enums.OrderStatus;
import org.lampis.common.event.BaseEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    private List<OrderLineDTO> orderLines;

    public OrderCreatedEvent(Long orderId, Long customerId, OrderStatus status, BigDecimal totalAmount, LocalDateTime orderDate, List<OrderLineDTO> orderLines) {
        super();
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.orderDate = orderDate;
        this.orderLines = orderLines;
        setEventType(EventType.ORDER_CREATED);
    }
}
