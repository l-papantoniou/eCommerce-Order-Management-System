package org.lampis.order.service;

import org.lampis.common.dto.order.*;
import org.lampis.common.enums.OrderStatus;
import org.lampis.common.event.order.OrderCreatedEvent;
import org.lampis.common.event.order.OrderStatusChangedEvent;
import org.lampis.common.exception.InsufficientStockException;
import org.lampis.common.exception.InvalidOrderStateException;
import org.lampis.common.exception.ResourceNotFoundException;
import org.lampis.order.entity.Inventory;
import org.lampis.order.entity.Order;
import org.lampis.order.entity.OrderAudit;
import org.lampis.order.entity.OrderLine;
import org.lampis.order.repository.InventoryRepository;
import org.lampis.order.repository.OrderAuditRepository;
import org.lampis.order.repository.OrderRepository;
import org.lampis.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderServiceImpl
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private OrderAuditRepository orderAuditRepository;

    @Mock
    private EventPublisherService eventPublisher;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order testOrder;
    private Inventory testInventory;
    private CreateOrderRequest createRequest;
    private UpdateOrderRequest updateRequest;

    @BeforeEach
    void setUp() {
        // Setup test inventory
        testInventory = Inventory.builder()
                .id(1L)
                .productId(1L)
                .productName("Test Product")
                .availableStock(100)
                .build();

        // Setup test order
        testOrder = Order.builder()
                .id(1L)
                .customerId(123L)
                .status(OrderStatus.UNPROCESSED)
                .orderDate(LocalDateTime.now())
                .totalAmount(new BigDecimal("59.98"))
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        OrderLine orderLine = OrderLine.builder()
                .id(1L)
                .order(testOrder)
                .productId(1L)
                .quantity(2)
                .unitPrice(new BigDecimal("29.99"))
                .lineTotal(new BigDecimal("59.98"))
                .build();

        testOrder.setOrderLines(new ArrayList<>(List.of(orderLine)));

        // Setup create request
        OrderLineDTO lineDTO = OrderLineDTO.builder()
                .productId(1L)
                .quantity(2)
                .unitPrice(new BigDecimal("29.99"))
                .build();

        createRequest = CreateOrderRequest.builder()
                .customerId(123L)
                .orderLines(List.of(lineDTO))
                .build();

        // Setup update request
        updateRequest = UpdateOrderRequest.builder()
                .orderLines(List.of(lineDTO))
                .build();
    }

    // ============== CREATE ORDER TESTS ==============


    @Test
    void createOrder_InsufficientStock_ThrowsException() {
        // Arrange
        Inventory lowStock = Inventory.builder()
                .id(1L)
                .productId(1L)
                .productName("Test Product")
                .availableStock(1) // Less than requested
                .build();

        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(lowStock));

        // Act & Assert
        InsufficientStockException exception = assertThrows(
                InsufficientStockException.class,
                () -> orderService.createOrder(createRequest)
        );

        assertEquals(1L, exception.getProductId());
        assertEquals(2, exception.getRequestedQuantity());
        assertEquals(1, exception.getAvailableStock());

        // Verify no order was saved
        verify(orderRepository, never()).save(any(Order.class));
        verify(eventPublisher, never()).publishOrderCreatedEvent(any(OrderCreatedEvent.class));
    }

    @Test
    void createOrder_ProductNotFound_ThrowsException() {
        // Arrange
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> orderService.createOrder(createRequest));

        // Verify no order was saved
        verify(orderRepository, never()).save(any(Order.class));
    }

    // ============== GET ORDER TESTS ==============

    @Test
    void getOrderById_Success() {
        // Arrange
        when(orderRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testOrder));

        // Act
        OrderResponse response = orderService.getOrderById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getOrderId());
        assertEquals(123L, response.getCustomerId());
        verify(orderRepository).findByIdAndNotDeleted(1L);
    }

    @Test
    void getOrderById_NotFound_ThrowsException() {
        // Arrange
        when(orderRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrderById(1L));
    }

    @Test
    void getAllOrders_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(testOrder));
        when(orderRepository.findAllNotDeleted(pageable)).thenReturn(orderPage);

        // Act
        Page<OrderResponse> response = orderService.getAllOrders(pageable);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        verify(orderRepository).findAllNotDeleted(pageable);
    }

    @Test
    void getOrdersByCustomerId_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(testOrder));
        when(orderRepository.findByCustomerId(123L, pageable)).thenReturn(orderPage);

        // Act
        Page<OrderResponse> response = orderService.getOrdersByCustomerId(123L, pageable);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        verify(orderRepository).findByCustomerId(123L, pageable);
    }

    @Test
    void getOrdersByStatus_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(testOrder));
        when(orderRepository.findByStatus(OrderStatus.UNPROCESSED, pageable)).thenReturn(orderPage);

        // Act
        Page<OrderResponse> response = orderService.getOrdersByStatus(OrderStatus.UNPROCESSED, pageable);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        verify(orderRepository).findByStatus(OrderStatus.UNPROCESSED, pageable);
    }

    // ============== UPDATE ORDER STATUS TESTS ==============

    @Test
    void updateOrderStatus_ValidTransition_Success() {
        // Arrange
        when(orderRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        OrderResponse response = orderService.updateOrderStatus(1L, OrderStatus.PROCESSING);

        // Assert
        assertNotNull(response);
        verify(orderAuditRepository).save(any(OrderAudit.class));
        verify(eventPublisher).publishOrderStatusChangedEvent(any(OrderStatusChangedEvent.class));
    }

    @Test
    void updateOrderStatus_InvalidTransition_ThrowsException() {
        // Arrange
        when(orderRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testOrder));

        // Act & Assert - trying to skip from UNPROCESSED to SHIPPED
        InvalidOrderStateException exception = assertThrows(
                InvalidOrderStateException.class,
                () -> orderService.updateOrderStatus(1L, OrderStatus.SHIPPED)
        );

        assertEquals(OrderStatus.UNPROCESSED, exception.getCurrentStatus());
        assertEquals(OrderStatus.SHIPPED, exception.getTargetStatus());

        // Verify no update happened
        verify(orderRepository, never()).save(any(Order.class));
        verify(eventPublisher, never()).publishOrderStatusChangedEvent(any(OrderStatusChangedEvent.class));
    }

    @Test
    void updateOrderStatus_FromProcessingToProcessed_Success() {
        // Arrange
        testOrder.setStatus(OrderStatus.PROCESSING);
        when(orderRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        OrderResponse response = orderService.updateOrderStatus(1L, OrderStatus.PROCESSED);

        // Assert
        assertNotNull(response);
        verify(eventPublisher).publishOrderStatusChangedEvent(any(OrderStatusChangedEvent.class));
    }

    @Test
    void updateOrderStatus_CancelUnprocessed_Success() {
        // Arrange
        when(orderRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        OrderResponse response = orderService.updateOrderStatus(1L, OrderStatus.CANCELLED);

        // Assert
        assertNotNull(response);
        verify(eventPublisher).publishOrderStatusChangedEvent(any(OrderStatusChangedEvent.class));
    }

    // ============== DELETE ORDER TESTS ==============

    @Test
    void deleteOrder_UnprocessedOrder_ReleasesInventory() {
        // Arrange
        when(orderRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testOrder));
        when(inventoryRepository.findByProductIdWithLock(1L)).thenReturn(Optional.of(testInventory));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        orderService.deleteOrder(1L);

        // Assert
        verify(inventoryRepository).findByProductIdWithLock(1L);
        verify(inventoryRepository).save(any(Inventory.class)); // Releasing stock
        verify(orderRepository).save(any(Order.class)); // Soft delete
        verify(orderAuditRepository).save(any(OrderAudit.class));
    }

    @Test
    void deleteOrder_ProcessedOrder_DoesNotReleaseInventory() {
        // Arrange
        testOrder.setStatus(OrderStatus.PROCESSED);
        when(orderRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        orderService.deleteOrder(1L);

        // Assert
        verify(inventoryRepository, never()).findByProductIdWithLock(anyLong());
        verify(orderRepository).save(any(Order.class)); // Still soft deletes
        verify(orderAuditRepository).save(any(OrderAudit.class));
    }

    @Test
    void deleteOrder_NotFound_ThrowsException() {
        // Arrange
        when(orderRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> orderService.deleteOrder(1L));
    }

    // ============== GET ORDER HISTORY TESTS ==============

    @Test
    void getOrderHistory_Success() {
        // Arrange
        OrderAudit audit = OrderAudit.builder()
                .id(1L)
                .orderId(1L)
                .fieldName("STATUS")
                .oldValue("UNPROCESSED")
                .newValue("PROCESSING")
                .changedAt(LocalDateTime.now())
                .changedBy("SYSTEM")
                .build();

        when(orderRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testOrder));
        when(orderAuditRepository.findByOrderIdOrderByChangedAtDesc(1L)).thenReturn(List.of(audit));

        // Act
        List<OrderAuditResponse> history = orderService.getOrderHistory(1L);

        // Assert
        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals("STATUS", history.get(0).getFieldName());
        verify(orderAuditRepository).findByOrderIdOrderByChangedAtDesc(1L);
    }

    @Test
    void getOrderHistory_OrderNotFound_ThrowsException() {
        // Arrange
        when(orderRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrderHistory(1L));
    }

    // ============== PROGRESS ORDER STATUSES TESTS ==============

    @Test
    void progressOrderStatuses_ProgressesAllStatuses() {
        // Arrange
        Order unprocessedOrder = Order.builder()
                .id(1L)
                .status(OrderStatus.UNPROCESSED)
                .customerId(123L)
                .deleted(false)
                .build();

        Order processingOrder = Order.builder()
                .id(2L)
                .status(OrderStatus.PROCESSING)
                .customerId(124L)
                .deleted(false)
                .build();

        Order processedOrder = Order.builder()
                .id(3L)
                .status(OrderStatus.PROCESSED)
                .customerId(125L)
                .deleted(false)
                .build();

        when(orderRepository.findByStatusForProcessing(OrderStatus.UNPROCESSED))
                .thenReturn(List.of(unprocessedOrder));
        when(orderRepository.findByStatusForProcessing(OrderStatus.PROCESSING))
                .thenReturn(List.of(processingOrder));
        when(orderRepository.findByStatusForProcessing(OrderStatus.PROCESSED))
                .thenReturn(List.of(processedOrder));
        when(orderRepository.findByIdAndNotDeleted(anyLong()))
                .thenReturn(Optional.of(unprocessedOrder), Optional.of(processingOrder), Optional.of(processedOrder));
        when(orderRepository.save(any(Order.class)))
                .thenReturn(unprocessedOrder, processingOrder, processedOrder);

        // Act
        orderService.progressOrderStatuses();

        // Assert
        verify(orderRepository).findByStatusForProcessing(OrderStatus.UNPROCESSED);
        verify(orderRepository).findByStatusForProcessing(OrderStatus.PROCESSING);
        verify(orderRepository).findByStatusForProcessing(OrderStatus.PROCESSED);
        verify(eventPublisher, times(3)).publishOrderStatusChangedEvent(any(OrderStatusChangedEvent.class));
    }

    @Test
    void progressOrderStatuses_NoOrders_DoesNothing() {
        // Arrange
        when(orderRepository.findByStatusForProcessing(any(OrderStatus.class)))
                .thenReturn(List.of());

        // Act
        orderService.progressOrderStatuses();

        // Assert
        verify(orderRepository, times(3)).findByStatusForProcessing(any(OrderStatus.class));
        verify(eventPublisher, never()).publishOrderStatusChangedEvent(any(OrderStatusChangedEvent.class));
    }
}