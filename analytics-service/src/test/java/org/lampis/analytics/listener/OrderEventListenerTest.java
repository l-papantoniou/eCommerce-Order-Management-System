package org.lampis.analytics.listener;

import org.lampis.analytics.service.AnalyticsService;
import org.lampis.common.dto.order.OrderLineDTO;
import org.lampis.common.enums.OrderStatus;
import org.lampis.common.event.order.OrderCreatedEvent;
import org.lampis.common.event.order.OrderStatusChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderEventListener
 */
@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private OrderEventListener orderEventListener;

    private OrderCreatedEvent orderCreatedEvent;
    private OrderStatusChangedEvent orderStatusChangedEvent;

    @BeforeEach
    void setUp() {
        List<OrderLineDTO> orderLines = List.of(
                OrderLineDTO.builder()
                        .productId(1L)
                        .quantity(2)
                        .unitPrice(new BigDecimal("29.99"))
                        .build()
        );

        orderCreatedEvent = new OrderCreatedEvent(
                1L,
                100L,
                OrderStatus.UNPROCESSED,
                new BigDecimal("59.98"),
                LocalDateTime.now(),
                orderLines
        );

        orderStatusChangedEvent = new OrderStatusChangedEvent(
                2L,
                200L,
                OrderStatus.PROCESSING,
                OrderStatus.PROCESSED
        );
    }

    @Test
    void handleOrderCreated_ProcessesEventSuccessfully() {
        // Arrange
        doNothing().when(analyticsService).processOrderCreated(
                anyLong(), anyLong(), any(OrderStatus.class),
                any(BigDecimal.class), any(LocalDateTime.class), anyList()
        );

        // Act
        orderEventListener.handleOrderCreated(orderCreatedEvent);

        // Assert
        verify(analyticsService).processOrderCreated(
                eq(1L),
                eq(100L),
                eq(OrderStatus.UNPROCESSED),
                eq(new BigDecimal("59.98")),
                any(LocalDateTime.class),
                anyList()
        );
    }

    @Test
    void handleOrderCreated_ConvertsOrderLinesToItems() {
        // Act
        orderEventListener.handleOrderCreated(orderCreatedEvent);

        // Assert
        verify(analyticsService).processOrderCreated(
                anyLong(), anyLong(), any(OrderStatus.class),
                any(BigDecimal.class), any(LocalDateTime.class),
                argThat(items -> {
                    // Verify items were converted correctly
                    return items.size() == 1 &&
                            items.get(0).getProductId() == 1L &&
                            items.get(0).getQuantity() == 2 &&
                            items.get(0).getUnitPrice().equals(new BigDecimal("29.99"));
                })
        );
    }

    @Test
    void handleOrderCreated_WhenExceptionThrown_ThrowsRuntimeException() {
        // Arrange
        doThrow(new RuntimeException("Database error"))
                .when(analyticsService)
                .processOrderCreated(anyLong(), anyLong(), any(), any(), any(), anyList());

        // Act & Assert
        try {
            orderEventListener.handleOrderCreated(orderCreatedEvent);
        } catch (RuntimeException e) {
            assertEquals("Failed to process order created analytics", e.getMessage());
        }

        verify(analyticsService).processOrderCreated(
                anyLong(), anyLong(), any(), any(), any(), anyList()
        );
    }

    @Test
    void handleOrderStatusChanged_ProcessesEventSuccessfully() {
        // Arrange
        doNothing().when(analyticsService).processOrderStatusChanged(
                anyLong(), anyLong(), any(OrderStatus.class), any(OrderStatus.class)
        );

        // Act
        orderEventListener.handleOrderStatusChanged(orderStatusChangedEvent);

        // Assert
        verify(analyticsService).processOrderStatusChanged(
                eq(2L),
                eq(200L),
                eq(OrderStatus.PROCESSING),
                eq(OrderStatus.PROCESSED)
        );
    }

    @Test
    void handleOrderStatusChanged_WhenExceptionThrown_ThrowsRuntimeException() {
        // Arrange
        doThrow(new RuntimeException("Database error"))
                .when(analyticsService)
                .processOrderStatusChanged(anyLong(), anyLong(), any(), any());

        // Act & Assert
        try {
            orderEventListener.handleOrderStatusChanged(orderStatusChangedEvent);
        } catch (RuntimeException e) {
            assertEquals("Failed to process order status change analytics", e.getMessage());
        }

        verify(analyticsService).processOrderStatusChanged(
                anyLong(), anyLong(), any(), any()
        );
    }

    @Test
    void handleOrderCreated_WithMultipleItems_ProcessesAll() {
        // Arrange
        List<OrderLineDTO> multipleItems = List.of(
                OrderLineDTO.builder()
                        .productId(1L)
                        .quantity(2)
                        .unitPrice(new BigDecimal("29.99"))
                        .build(),
                OrderLineDTO.builder()
                        .productId(2L)
                        .quantity(1)
                        .unitPrice(new BigDecimal("49.99"))
                        .build()
        );

        OrderCreatedEvent eventWithMultipleItems = new OrderCreatedEvent(
                3L,
                300L,
                OrderStatus.UNPROCESSED,
                new BigDecimal("109.97"),
                LocalDateTime.now(),
                multipleItems
        );

        // Act
        orderEventListener.handleOrderCreated(eventWithMultipleItems);

        // Assert
        verify(analyticsService).processOrderCreated(
                eq(3L),
                eq(300L),
                eq(OrderStatus.UNPROCESSED),
                eq(new BigDecimal("109.97")),
                any(LocalDateTime.class),
                argThat(items -> items.size() == 2)
        );
    }

    @Test
    void handleOrderCreated_CalculatesLineTotalCorrectly() {
        // Act
        orderEventListener.handleOrderCreated(orderCreatedEvent);

        // Assert
        verify(analyticsService).processOrderCreated(
                anyLong(), anyLong(), any(), any(), any(),
                argThat(items -> {
                    // Line total should be quantity * unit price
                    BigDecimal expectedTotal = new BigDecimal("29.99")
                            .multiply(BigDecimal.valueOf(2));
                    return items.get(0).getLineTotal().equals(expectedTotal);
                })
        );
    }
}