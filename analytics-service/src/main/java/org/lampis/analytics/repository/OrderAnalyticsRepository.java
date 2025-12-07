package org.lampis.analytics.repository;

import org.lampis.analytics.model.OrderAnalytics;
import org.lampis.common.enums.OrderStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for OrderAnalytics MongoDB operations
 */
@Repository
public interface OrderAnalyticsRepository extends MongoRepository<OrderAnalytics, String> {

    /**
     * Find by order ID
     */
    Optional<OrderAnalytics> findByOrderId(Long orderId);

    /**
     * Find by customer ID
     */
    List<OrderAnalytics> findByCustomerId(Long customerId);

    /**
     * Find by status
     */
    List<OrderAnalytics> findByStatus(OrderStatus status);

    /**
     * Find orders within date range
     */
    List<OrderAnalytics> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Count orders by status
     */
    long countByStatus(OrderStatus status);

}