package org.lampis.common.enums;

import lombok.Getter;

/**
 * Types of domain events
 */
@Getter
public enum EventType {
    ORDER_CREATED("Order has been created"),
    ORDER_UPDATED("Order has been updated"),
    ORDER_STATUS_CHANGED("Order status has changed"),
    ORDER_CANCELLED("Order has been cancelled"),
    ORDER_DELETED("Order has been deleted");

    private final String description;

    EventType(String description) {
        this.description = description;
    }
}
