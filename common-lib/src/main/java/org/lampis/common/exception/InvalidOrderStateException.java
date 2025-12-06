package org.lampis.common.exception;

import org.lampis.common.enums.OrderStatus;

/**
 * Exception thrown when an invalid order state transition is attempted
 */
public class InvalidOrderStateException extends BusinessException {

    private final OrderStatus currentStatus;
    private final OrderStatus targetStatus;

    public InvalidOrderStateException(OrderStatus currentStatus, OrderStatus targetStatus) {
        super("INVALID_ORDER_STATE",
                String.format("Cannot transition order from %s to %s", currentStatus, targetStatus));
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public InvalidOrderStateException(String message) {
        super("INVALID_ORDER_STATE", message);
        this.currentStatus = null;
        this.targetStatus = null;
    }

    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }

    public OrderStatus getTargetStatus() {
        return targetStatus;
    }
}
