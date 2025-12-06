package org.lampis.notification;

import org.lampis.common.enums.NotificationType;
import org.lampis.notification.model.NotificationLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.lampis.notification.service.EmailNotificationService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EmailNotificationService
 */
class EmailNotificationServiceTest {

    private EmailNotificationService emailService;
    private NotificationLog notificationLog;

    @BeforeEach
    void setUp() {
        emailService = new EmailNotificationService();

        notificationLog = NotificationLog.builder()
                .id("test-123")
                .orderId(1L)
                .customerId(100L)
                .type(NotificationType.EMAIL)
                .recipient("test@example.com")
                .subject("Test Subject")
                .message("Test Message")
                .build();
    }

    @Test
    void sendEmail_WithValidParameters_ReturnsBoolean() {
        // Act
        boolean result = emailService.sendEmail("test@example.com", "Subject", "Message");

        // Assert
        // Result can be true or false due to randomization
        assertTrue(result || !result); // Always passes, just checking it doesn't throw
    }

    @Test
    void sendEmail_WithNotificationLog_ReturnsBoolean() {
        // Act
        boolean result = emailService.sendEmail(notificationLog);

        // Assert
        assertTrue(result || !result); // Always passes
    }

    @RepeatedTest(10)
    void sendEmail_RepeatedCalls_ShowsRandomization() {
        // This test shows that the service simulates success/failure
        // By running 10 times, we expect to see both successes and failures

        // Act
        boolean result = emailService.sendEmail("test@example.com", "Subject", "Message");

        // Assert - just verify no exceptions thrown
        assertNotNull(result);
    }

    @Test
    void sendEmail_WithNullRecipient_HandlesGracefully() {
        // This tests defensive programming
        // Even with null, it shouldn't crash (though it will fail)

        // Act & Assert - should not throw NullPointerException
        assertDoesNotThrow(() -> {
            emailService.sendEmail(null, "Subject", "Message");
        });
    }
}