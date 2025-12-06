package org.lampis.common.enums;

import lombok.Getter;

/**
 * Types of notifications that can be sent
 */
@Getter
public enum NotificationType {
    EMAIL("Email notification"),
    SMS("SMS notification"),
    PUSH("Push notification");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

}
