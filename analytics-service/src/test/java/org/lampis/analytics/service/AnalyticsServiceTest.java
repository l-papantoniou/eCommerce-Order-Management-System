package org.lampis.analytics.service;

import org.lampis.analytics.model.CustomerAnalytics;
import org.lampis.analytics.model.DailyMetrics;
import org.lampis.analytics.model.OrderAnalytics;
import org.lampis.analytics.repository.CustomerAnalyticsRepository;
import org.lampis.analytics.repository.DailyMetricsRepository;
import org.lampis.analytics.repository.OrderAnalyticsRepository;
import org.lampis.common.enums.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AnalyticsService
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private OrderAnalyticsRepository orderAnalyticsRepository;

    @Mock
    private CustomerAnalyticsRepository customerAnalyticsRepository;

    @Mock
    private DailyMetricsRepository dailyMetricsRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private List<OrderAnalytics.OrderLineItem> orderItems;

    @BeforeEach
    void setUp() {
        orderItems = new ArrayList<>();
        orderItems.add(OrderAnalytics.OrderLineItem.builder()
                .productId(1L)
                .quantity(2)
                .unitPrice(new BigDecimal("29.99"))
                .lineTotal(new BigDecimal("59.98"))
                .build());
    }

    @Test
    void processOrderCreated_NewCustomer_CreatesAllAnalytics() {
        // Arrange
        Long orderId = 1L;
        Long customerId = 100L;
        OrderStatus status = OrderStatus.UNPROCESSED;
        BigDecimal totalAmount = new BigDecimal("59.98");
        LocalDateTime orderDate = LocalDateTime.now();

        when(customerAnalyticsRepository.findByCustomerId(customerId))
                .thenReturn(Optional.empty());
        when(dailyMetricsRepository.findByDate(orderDate.toLocalDate()))
                .thenReturn(Optional.empty());

        // Act
        analyticsService.processOrderCreated(orderId, customerId, status, totalAmount, orderDate, orderItems);

        // Assert
        // Verify OrderAnalytics saved
        ArgumentCaptor<OrderAnalytics> orderCaptor = ArgumentCaptor.forClass(OrderAnalytics.class);
        verify(orderAnalyticsRepository).save(orderCaptor.capture());
        OrderAnalytics savedOrder = orderCaptor.getValue();
        assertEquals(orderId, savedOrder.getOrderId());
        assertEquals(customerId, savedOrder.getCustomerId());
        assertEquals(totalAmount, savedOrder.getTotalAmount());
        assertEquals(1, savedOrder.getItemCount());

        // Verify CustomerAnalytics saved
        ArgumentCaptor<CustomerAnalytics> customerCaptor = ArgumentCaptor.forClass(CustomerAnalytics.class);
        verify(customerAnalyticsRepository).save(customerCaptor.capture());
        CustomerAnalytics savedCustomer = customerCaptor.getValue();
        assertEquals(customerId, savedCustomer.getCustomerId());
        assertEquals(1, savedCustomer.getTotalOrders());
        assertEquals(totalAmount, savedCustomer.getTotalRevenue());

        // Verify DailyMetrics saved
        ArgumentCaptor<DailyMetrics> metricsCaptor = ArgumentCaptor.forClass(DailyMetrics.class);
        verify(dailyMetricsRepository).save(metricsCaptor.capture());
        DailyMetrics savedMetrics = metricsCaptor.getValue();
        assertEquals(orderDate.toLocalDate(), savedMetrics.getDate());
        assertEquals(1, savedMetrics.getTotalOrders());
        assertEquals(totalAmount, savedMetrics.getTotalRevenue());
    }

    @Test
    void processOrderCreated_ExistingCustomer_UpdatesAnalytics() {
        // Arrange
        Long customerId = 100L;
        BigDecimal totalAmount = new BigDecimal("59.98");
        LocalDateTime orderDate = LocalDateTime.now();

        CustomerAnalytics existingCustomer = CustomerAnalytics.builder()
                .customerId(customerId)
                .totalOrders(5)
                .totalRevenue(new BigDecimal("500.00"))
                .ordersUnprocessed(0)
                .build();

        when(customerAnalyticsRepository.findByCustomerId(customerId))
                .thenReturn(Optional.of(existingCustomer));
        when(dailyMetricsRepository.findByDate(any(LocalDate.class)))
                .thenReturn(Optional.empty());

        // Act
        analyticsService.processOrderCreated(1L, customerId, OrderStatus.UNPROCESSED, totalAmount, orderDate, orderItems);

        // Assert
        ArgumentCaptor<CustomerAnalytics> captor = ArgumentCaptor.forClass(CustomerAnalytics.class);
        verify(customerAnalyticsRepository).save(captor.capture());
        CustomerAnalytics updated = captor.getValue();

        assertEquals(6, updated.getTotalOrders());
        assertEquals(new BigDecimal("559.98"), updated.getTotalRevenue());
        assertEquals(1, updated.getOrdersUnprocessed());
    }

    @Test
    void processOrderStatusChanged_UpdatesOrderAndCustomerAnalytics() {
        // Arrange
        Long orderId = 1L;
        Long customerId = 100L;
        OrderStatus oldStatus = OrderStatus.UNPROCESSED;
        OrderStatus newStatus = OrderStatus.PROCESSING;

        OrderAnalytics existingOrder = OrderAnalytics.builder()
                .orderId(orderId)
                .customerId(customerId)
                .status(oldStatus)
                .build();

        CustomerAnalytics existingCustomer = CustomerAnalytics.builder()
                .customerId(customerId)
                .ordersUnprocessed(5)
                .ordersProcessing(2)
                .build();

        when(orderAnalyticsRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(existingOrder));
        when(customerAnalyticsRepository.findByCustomerId(customerId))
                .thenReturn(Optional.of(existingCustomer));

        // Act
        analyticsService.processOrderStatusChanged(orderId, customerId, oldStatus, newStatus);

        // Assert
        // Verify order status updated
        ArgumentCaptor<OrderAnalytics> orderCaptor = ArgumentCaptor.forClass(OrderAnalytics.class);
        verify(orderAnalyticsRepository).save(orderCaptor.capture());
        assertEquals(newStatus, orderCaptor.getValue().getStatus());

        // Verify customer status counts updated
        ArgumentCaptor<CustomerAnalytics> customerCaptor = ArgumentCaptor.forClass(CustomerAnalytics.class);
        verify(customerAnalyticsRepository).save(customerCaptor.capture());
        CustomerAnalytics updated = customerCaptor.getValue();
        assertEquals(4, updated.getOrdersUnprocessed()); // Decremented
        assertEquals(3, updated.getOrdersProcessing());  // Incremented
    }

    @Test
    void getOrderAnalytics_ReturnsOrder() {
        // Arrange
        Long orderId = 1L;
        OrderAnalytics orderAnalytics = OrderAnalytics.builder()
                .orderId(orderId)
                .build();

        when(orderAnalyticsRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(orderAnalytics));

        // Act
        OrderAnalytics result = analyticsService.getOrderAnalytics(orderId);

        // Assert
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        verify(orderAnalyticsRepository).findByOrderId(orderId);
    }

    @Test
    void getOrderAnalytics_NotFound_ReturnsNull() {
        // Arrange
        when(orderAnalyticsRepository.findByOrderId(anyLong()))
                .thenReturn(Optional.empty());

        // Act
        OrderAnalytics result = analyticsService.getOrderAnalytics(999L);

        // Assert
        assertNull(result);
    }

    @Test
    void getCustomerAnalytics_ReturnsCustomer() {
        // Arrange
        Long customerId = 100L;
        CustomerAnalytics customerAnalytics = CustomerAnalytics.builder()
                .customerId(customerId)
                .totalOrders(10)
                .totalRevenue(new BigDecimal("1000.00"))
                .build();

        when(customerAnalyticsRepository.findByCustomerId(customerId))
                .thenReturn(Optional.of(customerAnalytics));

        // Act
        CustomerAnalytics result = analyticsService.getCustomerAnalytics(customerId);

        // Assert
        assertNotNull(result);
        assertEquals(customerId, result.getCustomerId());
        assertEquals(10, result.getTotalOrders());
    }

    @Test
    void getOrdersByStatus_ReturnsFilteredOrders() {
        // Arrange
        OrderStatus status = OrderStatus.SHIPPED;
        List<OrderAnalytics> orders = List.of(
                OrderAnalytics.builder().orderId(1L).status(status).build(),
                OrderAnalytics.builder().orderId(2L).status(status).build()
        );

        when(orderAnalyticsRepository.findByStatus(status)).thenReturn(orders);

        // Act
        List<OrderAnalytics> result = analyticsService.getOrdersByStatus(status);

        // Assert
        assertEquals(2, result.size());
        verify(orderAnalyticsRepository).findByStatus(status);
    }

    @Test
    void getTopCustomersByRevenue_ReturnsLimitedList() {
        // Arrange
        List<CustomerAnalytics> customers = List.of(
                CustomerAnalytics.builder()
                        .customerId(1L)
                        .totalRevenue(new BigDecimal("1000"))
                        .build(),
                CustomerAnalytics.builder()
                        .customerId(2L)
                        .totalRevenue(new BigDecimal("900"))
                        .build(),
                CustomerAnalytics.builder()
                        .customerId(3L)
                        .totalRevenue(new BigDecimal("800"))
                        .build()
        );

        when(customerAnalyticsRepository.findTopCustomersByRevenue())
                .thenReturn(customers);

        // Act
        List<CustomerAnalytics> result = analyticsService.getTopCustomersByRevenue(2);

        // Assert
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getCustomerId());
        assertEquals(2L, result.get(1).getCustomerId());
    }

    @Test
    void getDailyMetrics_ReturnsDateRange() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 12, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 7);
        List<DailyMetrics> metrics = List.of(
                DailyMetrics.builder().date(LocalDate.of(2024, 12, 1)).build(),
                DailyMetrics.builder().date(LocalDate.of(2024, 12, 2)).build()
        );

        when(dailyMetricsRepository.findByDateBetweenOrderByDateAsc(startDate, endDate))
                .thenReturn(metrics);

        // Act
        List<DailyMetrics> result = analyticsService.getDailyMetrics(startDate, endDate);

        // Assert
        assertEquals(2, result.size());
        verify(dailyMetricsRepository).findByDateBetweenOrderByDateAsc(startDate, endDate);
    }

    @Test
    void getOrderCountByStatus_ReturnsCount() {
        // Arrange
        OrderStatus status = OrderStatus.SHIPPED;
        when(orderAnalyticsRepository.countByStatus(status)).thenReturn(42L);

        // Act
        long count = analyticsService.getOrderCountByStatus(status);

        // Assert
        assertEquals(42L, count);
        verify(orderAnalyticsRepository).countByStatus(status);
    }

    @Test
    void getTotalRevenue_CalculatesCorrectly() {
        // Arrange
        List<CustomerAnalytics> customers = List.of(
                CustomerAnalytics.builder()
                        .totalRevenue(new BigDecimal("100.00"))
                        .build(),
                CustomerAnalytics.builder()
                        .totalRevenue(new BigDecimal("200.00"))
                        .build(),
                CustomerAnalytics.builder()
                        .totalRevenue(new BigDecimal("300.00"))
                        .build()
        );

        when(customerAnalyticsRepository.findAll()).thenReturn(customers);

        // Act
        BigDecimal totalRevenue = analyticsService.getTotalRevenue();

        // Assert
        assertEquals(new BigDecimal("600.00"), totalRevenue);
    }
}