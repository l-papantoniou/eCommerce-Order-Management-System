package org.lampis.notification;

import org.lampis.common.enums.NotificationType;
import org.lampis.notification.model.NotificationLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lampis.notification.service.EmailNotificationService;
import org.lampis.notification.service.NotificationService;
import org.lampis.notification.service.SmsNotificationService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationService
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private EmailNotificationService emailService;

    @Mock
    private SmsNotificationService smsService;

    @InjectMocks
    private NotificationService notificationService;

    private NotificationLog emailNotification;
    private NotificationLog smsNotification;

    @BeforeEach
    void setUp() {
        emailNotification = NotificationLog.builder()
                .id("email-123")
                .orderId(1L)
                .customerId(100L)
                .type(NotificationType.EMAIL)
                .recipient("test@example.com")
                .subject("Test Email")
                .message("Test message")
                .status(NotificationLog.NotificationStatus.PENDING)
                .attemptCount(0)
                .build();

        smsNotification = NotificationLog.builder()
                .id("sms-456")
                .orderId(2L)
                .customerId(200L)
                .type(NotificationType.SMS)
                .recipient("+1234567890")
                .message("Test SMS")
                .status(NotificationLog.NotificationStatus.PENDING)
                .attemptCount(0)
                .build();
    }

    @Test
    void sendNotification_Email_Success() {
        // Arrange
        when(emailService.sendEmail(any(NotificationLog.class))).thenReturn(true);

        // Act
        NotificationLog result = notificationService.sendNotification(emailNotification);

        // Assert
        assertNotNull(result);
        assertEquals(NotificationLog.NotificationStatus.SENT, result.getStatus());
        assertEquals(1, result.getAttemptCount());
        assertNotNull(result.getSentAt());
        verify(emailService).sendEmail(any(NotificationLog.class));
    }

    @Test
    void sendNotification_Sms_Success() {
        // Arrange
        when(smsService.sendSms(any(NotificationLog.class))).thenReturn(true);

        // Act
        NotificationLog result = notificationService.sendNotification(smsNotification);

        // Assert
        assertNotNull(result);
        assertEquals(NotificationLog.NotificationStatus.SENT, result.getStatus());
        assertEquals(1, result.getAttemptCount());
        assertNotNull(result.getSentAt());
        verify(smsService).sendSms(any(NotificationLog.class));
    }

    @Test
    void sendNotification_Email_Failure_TriggersRetry() {
        // Arrange
        when(emailService.sendEmail(any(NotificationLog.class))).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            notificationService.sendNotification(emailNotification);
        });

        // Verify attempt was incremented
        assertEquals(1, emailNotification.getAttemptCount());
        verify(emailService).sendEmail(any(NotificationLog.class));
    }

    @Test
    void createOrderCreatedNotification_CreatesCorrectNotification() {
        // Act
        NotificationLog result = notificationService.createOrderCreatedNotification(123L, 456L);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(123L, result.getOrderId());
        assertEquals(456L, result.getCustomerId());
        assertEquals(NotificationType.EMAIL, result.getType());
        assertEquals("customer456@example.com", result.getRecipient());
        assertTrue(result.getSubject().contains("Order #123"));
        assertEquals(NotificationLog.NotificationStatus.PENDING, result.getStatus());
        assertEquals(0, result.getAttemptCount());
    }

    @Test
    void createStatusChangeNotification_CreatesCorrectNotification() {
        // Act
        NotificationLog result = notificationService.createStatusChangeNotification(
                789L,
                999L,
                "PROCESSING",
                "PROCESSED"
        );

        // Assert
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(789L, result.getOrderId());
        assertEquals(999L, result.getCustomerId());
        assertEquals(NotificationType.SMS, result.getType());
        assertEquals("+1234567890999", result.getRecipient());
        assertTrue(result.getMessage().contains("PROCESSING"));
        assertTrue(result.getMessage().contains("PROCESSED"));
        assertEquals(NotificationLog.NotificationStatus.PENDING, result.getStatus());
    }

    @Test
    void getNotification_ReturnsStoredNotification() {
        // Arrange
        when(emailService.sendEmail(any(NotificationLog.class))).thenReturn(true);
        notificationService.sendNotification(emailNotification);

        // Act
        NotificationLog result = notificationService.getNotification(emailNotification.getId());

        // Assert
        assertNotNull(result);
        assertEquals(emailNotification.getId(), result.getId());
        assertEquals(NotificationLog.NotificationStatus.SENT, result.getStatus());
    }

    @Test
    void getAllNotifications_ReturnsAllStoredNotifications() {
        // Arrange
        when(emailService.sendEmail(any(NotificationLog.class))).thenReturn(true);
        when(smsService.sendSms(any(NotificationLog.class))).thenReturn(true);

        notificationService.sendNotification(emailNotification);
        notificationService.sendNotification(smsNotification);

        // Act
        var result = notificationService.getAllNotifications();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey(emailNotification.getId()));
        assertTrue(result.containsKey(smsNotification.getId()));
    }

    @Test
    void recoverFromFailure_MarksNotificationAsFailed() {
        // Arrange
        RuntimeException exception = new RuntimeException("Test failure");

        // Act
        NotificationLog result = notificationService.recoverFromFailure(exception, emailNotification);

        // Assert
        assertEquals(NotificationLog.NotificationStatus.FAILED, result.getStatus());
        assertEquals("Test failure", result.getErrorMessage());
    }
}