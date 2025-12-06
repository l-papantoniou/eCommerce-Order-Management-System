package org.lampis.notification.model;


import lombok.*;
import org.lampis.common.enums.NotificationType;

import java.time.LocalDateTime;

/**
 * Model to track notification attempts
 * In a real system, this would be persisted to a database
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLog {

    private String id;
    private Long orderId;
    private Long customerId;
    private NotificationType type;
    private String recipient;
    private String subject;
    private String message;
    private NotificationStatus status;
    private Integer attemptCount;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime lastAttemptAt;

    public enum NotificationStatus {
        PENDING,
        SENT,
        FAILED,
        RETRY
    }

    /**
     * Increment attempt count
     */
    public void incrementAttempt() {
        this.attemptCount = (this.attemptCount == null ? 0 : this.attemptCount) + 1;
        this.lastAttemptAt = LocalDateTime.now();
    }

    /**
     * Mark as sent
     */
    public void markAsSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    /**
     * Mark as failed
     */
    public void markAsFailed(String errorMessage) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    /**
     * Mark for retry
     */
    public void markForRetry() {
        this.status = NotificationStatus.RETRY;
    }
}