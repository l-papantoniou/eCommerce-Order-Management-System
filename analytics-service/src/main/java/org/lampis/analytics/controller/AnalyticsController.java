package org.lampis.analytics.controller;

import org.lampis.analytics.model.CustomerAnalytics;
import org.lampis.analytics.model.DailyMetrics;
import org.lampis.analytics.model.OrderAnalytics;
import org.lampis.analytics.service.AnalyticsService;
import org.lampis.common.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for analytics endpoints
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Get order analytics by order ID
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderAnalytics> getOrderAnalytics(@PathVariable Long orderId) {
        log.info("Fetching order analytics for orderId: {}", orderId);
        OrderAnalytics analytics = analyticsService.getOrderAnalytics(orderId);

        if (analytics == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(analytics);
    }

    /**
     * Get customer analytics by customer ID
     */
    @GetMapping("/customers/{customerId}")
    public ResponseEntity<CustomerAnalytics> getCustomerAnalytics(@PathVariable Long customerId) {
        log.info("Fetching customer analytics for customerId: {}", customerId);
        CustomerAnalytics analytics = analyticsService.getCustomerAnalytics(customerId);

        if (analytics == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(analytics);
    }

    /**
     * Get orders by status
     */
    @GetMapping("/orders/status/{status}")
    public ResponseEntity<List<OrderAnalytics>> getOrdersByStatus(@PathVariable OrderStatus status) {
        log.info("Fetching orders by status: {}", status);
        List<OrderAnalytics> orders = analyticsService.getOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get top customers by revenue
     */
    @GetMapping("/customers/top-revenue")
    public ResponseEntity<List<CustomerAnalytics>> getTopCustomersByRevenue(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Fetching top {} customers by revenue", limit);
        List<CustomerAnalytics> customers = analyticsService.getTopCustomersByRevenue(limit);
        return ResponseEntity.ok(customers);
    }

    /**
     * Get daily metrics for date range
     */
    @GetMapping("/metrics/daily")
    public ResponseEntity<List<DailyMetrics>> getDailyMetrics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("Fetching daily metrics from {} to {}", startDate, endDate);
        List<DailyMetrics> metrics = analyticsService.getDailyMetrics(startDate, endDate);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get recent metrics (last 30 days)
     */
    @GetMapping("/metrics/recent")
    public ResponseEntity<List<DailyMetrics>> getRecentMetrics() {
        log.info("Fetching recent metrics");
        List<DailyMetrics> metrics = analyticsService.getRecentMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get summary statistics
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        log.info("Fetching analytics summary");

        Map<String, Object> summary = new HashMap<>();

        // Order counts by status
        summary.put("totalUnprocessed", analyticsService.getOrderCountByStatus(OrderStatus.UNPROCESSED));
        summary.put("totalProcessing", analyticsService.getOrderCountByStatus(OrderStatus.PROCESSING));
        summary.put("totalProcessed", analyticsService.getOrderCountByStatus(OrderStatus.PROCESSED));
        summary.put("totalShipped", analyticsService.getOrderCountByStatus(OrderStatus.SHIPPED));
        summary.put("totalCancelled", analyticsService.getOrderCountByStatus(OrderStatus.CANCELLED));

        // Revenue
        summary.put("totalRevenue", analyticsService.getTotalRevenue());

        // Recent metrics
        List<DailyMetrics> recentMetrics = analyticsService.getRecentMetrics();
        if (!recentMetrics.isEmpty()) {
            DailyMetrics latest = recentMetrics.get(0);
            summary.put("todayOrders", latest.getTotalOrders());
            summary.put("todayRevenue", latest.getTotalRevenue());
            summary.put("todayAverageOrderValue", latest.getAverageOrderValue());
        }

        return ResponseEntity.ok(summary);
    }

    /**
     * Get order count by status
     */
    @GetMapping("/orders/count/{status}")
    public ResponseEntity<Map<String, Long>> getOrderCountByStatus(@PathVariable OrderStatus status) {
        log.info("Fetching order count for status: {}", status);
        long count = analyticsService.getOrderCountByStatus(status);

        Map<String, Long> response = new HashMap<>();
        response.put("status", count);
        response.put("count", count);

        return ResponseEntity.ok(response);
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "analytics-service");
        return ResponseEntity.ok(response);
    }
}