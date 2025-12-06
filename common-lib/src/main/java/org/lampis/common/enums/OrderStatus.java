package org.lampis.common.enums;

import lombok.Getter;

/**
 * Order lifecycle status
 * Flow: UNPROCESSED -> PROCESSING -> PROCESSED -> SHIPPED
 */
@Getter
public enum OrderStatus {
    UNPROCESSED("Order created, awaiting processing"),
    PROCESSING("Order is being processed"),
    PROCESSED("Order has been processed"),
    SHIPPED("Order has been shipped"),
    CANCELLED("Order has been cancelled");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    /**
     * Get the next status in the lifecycle
     */
    public OrderStatus getNextStatus() {
        return switch (this) {
            case UNPROCESSED -> PROCESSING;
            case PROCESSING -> PROCESSED;
            case PROCESSED -> SHIPPED;
            case SHIPPED, CANCELLED -> this; // Terminal states
        };
    }

    /**
     * Check if this is a terminal status
     */
    public boolean isTerminal() {
        return this == SHIPPED || this == CANCELLED;
    }

    /**
     * Check if transition to target status is valid
     */
    public boolean canTransitionTo(OrderStatus target) {
        if (this == target) {
            return true;
        }
        if (this.isTerminal()) {
            return false;
        }
        // Can always cancel (except if already cancelled or shipped)
        if (target == CANCELLED && this != SHIPPED) {
            return true;
        }
        // Normal progression
        return target == this.getNextStatus();
    }
}