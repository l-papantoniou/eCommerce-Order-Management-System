package org.lampis.analytics.service;

import org.lampis.analytics.model.CustomerAnalytics;
import org.lampis.analytics.model.DailyMetrics;
import org.lampis.analytics.model.OrderAnalytics;
import org.lampis.analytics.repository.CustomerAnalyticsRepository;
import org.lampis.analytics.repository.DailyMetricsRepository;
import org.lampis.analytics.repository.OrderAnalyticsRepository;
import org.lampis.common.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for analytics operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final OrderAnalyticsRepository orderAnalyticsRepository;
    private final CustomerAnalyticsRepository customerAnalyticsRepository;
    private final DailyMetricsRepository dailyMetricsRepository;

    /**
     * Process order created event
     */
    @Transactional
    public void processOrderCreated(Long orderId, Long customerId, OrderStatus status,
                                    BigDecimal totalAmount, LocalDateTime orderDate,
                                    List<OrderAnalytics.OrderLineItem> items) {
        log.info("Processing order created analytics for order: {}", orderId);

        // Create order analytics
        OrderAnalytics orderAnalytics = OrderAnalytics.builder()
                .orderId(orderId)
                .customerId(customerId)
                .status(status)
                .totalAmount(totalAmount)
                .orderDate(orderDate)
                .itemCount(items.size())
                .items(items)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        orderAnalyticsRepository.save(orderAnalytics);
        log.debug("Order analytics saved: {}", orderId);

        // Update customer analytics
        updateCustomerAnalytics(customerId, totalAmount, status, orderDate);

        // Update daily metrics
        updateDailyMetrics(orderDate.toLocalDate(), totalAmount);

        log.info("Analytics processing completed for order: {}", orderId);
    }

    /**
     * Process order status change event
     */
    @Transactional
    public void processOrderStatusChanged(Long orderId, Long customerId,
                                          OrderStatus oldStatus, OrderStatus newStatus) {
        log.info("Processing order status change for order: {} ({} -> {})",
                orderId, oldStatus, newStatus);

        // Update order analytics
        orderAnalyticsRepository.findByOrderId(orderId)
                .ifPresent(orderAnalytics -> {
                    orderAnalytics.updateStatus(newStatus);
                    orderAnalyticsRepository.save(orderAnalytics);
                });

        // Update customer analytics
        customerAnalyticsRepository.findByCustomerId(customerId)
                .ifPresent(customerAnalytics -> {
                    customerAnalytics.decrementStatusCount(oldStatus);
                    customerAnalytics.incrementStatusCount(newStatus);
                    customerAnalyticsRepository.save(customerAnalytics);
                });

        // Update daily metrics if status reached terminal state
        if (newStatus == OrderStatus.PROCESSED || newStatus == OrderStatus.SHIPPED ||
                newStatus == OrderStatus.CANCELLED) {
            updateDailyMetricsForStatusChange(newStatus);
        }

        log.info("Status change analytics updated for order: {}", orderId);
    }

    /**
     * Update customer analytics
     */
    private void updateCustomerAnalytics(Long customerId, BigDecimal orderAmount,
                                         OrderStatus status, LocalDateTime orderDate) {
        CustomerAnalytics customerAnalytics = customerAnalyticsRepository
                .findByCustomerId(customerId)
                .orElse(CustomerAnalytics.builder()
                        .customerId(customerId)
                        .totalOrders(0)
                        .totalRevenue(BigDecimal.ZERO)
                        .firstOrderDate(orderDate)
                        .createdAt(LocalDateTime.now())
                        .build());

        customerAnalytics.addOrder(orderAmount);
        customerAnalytics.incrementStatusCount(status);

        if (customerAnalytics.getFirstOrderDate() == null) {
            customerAnalytics.setFirstOrderDate(orderDate);
        }

        customerAnalyticsRepository.save(customerAnalytics);
        log.debug("Customer analytics updated for customer: {}", customerId);
    }

    /**
     * Update daily metrics
     */
    private void updateDailyMetrics(LocalDate date, BigDecimal orderAmount) {
        DailyMetrics dailyMetrics = dailyMetricsRepository
                .findByDate(date)
                .orElse(DailyMetrics.builder()
                        .date(date)
                        .totalOrders(0)
                        .totalRevenue(BigDecimal.ZERO)
                        .createdAt(LocalDateTime.now())
                        .build());

        dailyMetrics.addOrder(orderAmount);
        dailyMetricsRepository.save(dailyMetrics);
        log.debug("Daily metrics updated for date: {}", date);
    }

    /**
     * Update daily metrics for status change
     */
    private void updateDailyMetricsForStatusChange(OrderStatus status) {
        LocalDate today = LocalDate.now();
        dailyMetricsRepository.findByDate(today)
                .ifPresent(dailyMetrics -> {
                    switch (status) {
                        case PROCESSED -> dailyMetrics.incrementProcessed();
                        case SHIPPED -> dailyMetrics.incrementShipped();
                        case CANCELLED -> dailyMetrics.incrementCancelled();
                    }
                    dailyMetricsRepository.save(dailyMetrics);
                });
    }

    /**
     * Get order analytics by ID
     */
    public OrderAnalytics getOrderAnalytics(Long orderId) {
        return orderAnalyticsRepository.findByOrderId(orderId)
                .orElse(null);
    }

    /**
     * Get customer analytics
     */
    public CustomerAnalytics getCustomerAnalytics(Long customerId) {
        return customerAnalyticsRepository.findByCustomerId(customerId)
                .orElse(null);
    }

    /**
     * Get orders by status
     */
    public List<OrderAnalytics> getOrdersByStatus(OrderStatus status) {
        return orderAnalyticsRepository.findByStatus(status);
    }

    /**
     * Get top customers by revenue
     */
    public List<CustomerAnalytics> getTopCustomersByRevenue(int limit) {
        return customerAnalyticsRepository.findTopCustomersByRevenue()
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get daily metrics for date range
     */
    public List<DailyMetrics> getDailyMetrics(LocalDate startDate, LocalDate endDate) {
        return dailyMetricsRepository.findByDateBetweenOrderByDateAsc(startDate, endDate);
    }

    /**
     * Get recent metrics (last 30 days)
     */
    public List<DailyMetrics> getRecentMetrics() {
        return dailyMetricsRepository.findTop30ByOrderByDateDesc();
    }

    /**
     * Get total revenue
     */
    public BigDecimal getTotalRevenue() {
        return customerAnalyticsRepository.findAll()
                .stream()
                .map(CustomerAnalytics::getTotalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get order count by status
     */
    public long getOrderCountByStatus(OrderStatus status) {
        return orderAnalyticsRepository.countByStatus(status);
    }
}