package org.lampis.notification.service;

import org.lampis.common.enums.NotificationType;
import org.lampis.notification.model.NotificationLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Orchestrator service for handling notifications with retry logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final EmailNotificationService emailService;
    private final SmsNotificationService smsService;

    // In-memory storage for demo (in production, use database)
    private final Map<String, NotificationLog> notificationStore = new ConcurrentHashMap<>();

    @Value("${notification.max-attempts:3}")
    private int maxAttempts;

    /**
     * Send notification with retry logic
     */
    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2),
            retryFor = {RuntimeException.class}
    )
    public NotificationLog sendNotification(NotificationLog notificationLog) {
        log.info("Sending {} notification for order: {}, attempt: {}",
                notificationLog.getType(),
                notificationLog.getOrderId(),
                notificationLog.getAttemptCount() + 1);

        notificationLog.incrementAttempt();

        boolean success = false;
        try {
            if (notificationLog.getType() == NotificationType.EMAIL) {
                success = emailService.sendEmail(notificationLog);
            } else if (notificationLog.getType() == NotificationType.SMS) {
                success = smsService.sendSms(notificationLog);
            }

            if (success) {
                notificationLog.markAsSent();
                log.info("Notification sent successfully: {}", notificationLog.getId());
            } else {
                // Will trigger retry
                throw new RuntimeException("Notification failed for type: " + notificationLog.getType());
            }

        } catch (Exception e) {
            log.error("Error sending notification: {}", notificationLog.getId(), e);

            if (notificationLog.getAttemptCount() >= maxAttempts) {
                notificationLog.markAsFailed(e.getMessage());
                log.error("Max retry attempts reached for notification: {}", notificationLog.getId());
            } else {
                notificationLog.markForRetry();
            }

            throw e; // Re-throw to trigger @Retryable
        } finally {
            notificationStore.put(notificationLog.getId(), notificationLog);
        }

        return notificationLog;
    }

    /**
     * Recover method - called when all retries are exhausted
     */
    @Recover
    public NotificationLog recoverFromFailure(RuntimeException e, NotificationLog notificationLog) {
        log.error("All retry attempts exhausted for notification: {}", notificationLog.getId());
        notificationLog.markAsFailed(e.getMessage());
        notificationStore.put(notificationLog.getId(), notificationLog);
        return notificationLog;
    }

    /**
     * Create notification log for order created
     */
    public NotificationLog createOrderCreatedNotification(Long orderId, Long customerId) {
        return NotificationLog.builder()
                .id(UUID.randomUUID().toString())
                .orderId(orderId)
                .customerId(customerId)
                .type(NotificationType.EMAIL)
                .recipient("customer" + customerId + "@example.com")
                .subject("Order Confirmation - Order #" + orderId)
                .message("Your order #" + orderId + " has been received and is being processed.")
                .status(NotificationLog.NotificationStatus.PENDING)
                .attemptCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Create notification log for status change
     */
    public NotificationLog createStatusChangeNotification(Long orderId, Long customerId, String oldStatus, String newStatus) {
        return NotificationLog.builder()
                .id(UUID.randomUUID().toString())
                .orderId(orderId)
                .customerId(customerId)
                .type(NotificationType.SMS)
                .recipient("+1234567890" + customerId) // Simulated phone
                .subject(null) // SMS doesn't have subject
                .message(String.format("Order #%d status updated: %s → %s", orderId, oldStatus, newStatus))
                .status(NotificationLog.NotificationStatus.PENDING)
                .attemptCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Get notification by ID
     */
    public NotificationLog getNotification(String id) {
        return notificationStore.get(id);
    }

    /**
     * Get all notifications
     */
    public Map<String, NotificationLog> getAllNotifications() {
        return new ConcurrentHashMap<>(notificationStore);
    }
}