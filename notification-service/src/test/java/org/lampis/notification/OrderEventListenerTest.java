package org.lampis.notification;

import org.lampis.common.enums.OrderStatus;
import org.lampis.common.event.order.OrderCreatedEvent;
import org.lampis.common.event.order.OrderStatusChangedEvent;
import org.lampis.notification.listener.OrderEventListener;
import org.lampis.notification.model.NotificationLog;
import org.lampis.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderEventListener
 */
@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderEventListener orderEventListener;

    private OrderCreatedEvent orderCreatedEvent;
    private OrderStatusChangedEvent orderStatusChangedEvent;
    private NotificationLog mockNotification;

    @BeforeEach
    void setUp() {
        orderCreatedEvent = new OrderCreatedEvent(
                1L,
                100L,
                OrderStatus.UNPROCESSED,
                new BigDecimal("99.99"),
                LocalDateTime.now()
        );

        orderStatusChangedEvent = new OrderStatusChangedEvent(
                2L,
                200L,
                OrderStatus.PROCESSING,
                OrderStatus.PROCESSED
        );

        mockNotification = NotificationLog.builder()
                .id("test-123")
                .orderId(1L)
                .customerId(100L)
                .status(NotificationLog.NotificationStatus.PENDING)
                .build();
    }

    @Test
    void handleOrderCreated_SendsEmailAndSmsNotifications() {
        // Arrange
        when(notificationService.createOrderCreatedNotification(1L, 100L))
                .thenReturn(mockNotification);
        when(notificationService.sendNotification(any(NotificationLog.class)))
                .thenReturn(mockNotification);

        // Act
        orderEventListener.handleOrderCreated(orderCreatedEvent);

        // Assert
        verify(notificationService).createOrderCreatedNotification(1L, 100L);
        verify(notificationService, times(2)).sendNotification(any(NotificationLog.class));
    }

    @Test
    void handleOrderCreated_WhenExceptionThrown_LogsError() {
        // Arrange
        when(notificationService.createOrderCreatedNotification(anyLong(), anyLong()))
                .thenThrow(new RuntimeException("Test exception"));

        // Act & Assert - should not throw, just log
        orderEventListener.handleOrderCreated(orderCreatedEvent);

        // Verify it tried to create notification
        verify(notificationService).createOrderCreatedNotification(1L, 100L);
    }

    @Test
    void handleOrderStatusChanged_SendsStatusChangeNotification() {
        // Arrange
        when(notificationService.createStatusChangeNotification(
                eq(2L), eq(200L), eq("PROCESSING"), eq("PROCESSED")))
                .thenReturn(mockNotification);
        when(notificationService.sendNotification(any(NotificationLog.class)))
                .thenReturn(mockNotification);

        // Act
        orderEventListener.handleOrderStatusChanged(orderStatusChangedEvent);

        // Assert
        verify(notificationService).createStatusChangeNotification(
                2L, 200L, "PROCESSING", "PROCESSED");
        verify(notificationService).sendNotification(any(NotificationLog.class));
    }

    @Test
    void handleOrderStatusChanged_WhenExceptionThrown_LogsError() {
        // Arrange
        when(notificationService.createStatusChangeNotification(
                anyLong(), anyLong(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Test exception"));

        // Act & Assert - should not throw, just log
        orderEventListener.handleOrderStatusChanged(orderStatusChangedEvent);

        // Verify it tried to create notification
        verify(notificationService).createStatusChangeNotification(
                2L, 200L, "PROCESSING", "PROCESSED");
    }

    @Test
    void handleDeadLetterQueue_LogsMessage() {
        // Arrange
        org.springframework.amqp.core.Message message =
                new org.springframework.amqp.core.Message("test message".getBytes());

        // Act & Assert - should not throw
        orderEventListener.handleDeadLetterQueue(message);

        // This test mainly ensures the DLQ handler doesn't crash
        // In production, you'd verify logging or alerting
    }
}