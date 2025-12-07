package org.lampis.analytics.repository;

import org.lampis.analytics.model.CustomerAnalytics;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository for CustomerAnalytics MongoDB operations
 */
@Repository
public interface CustomerAnalyticsRepository extends MongoRepository<CustomerAnalytics, String> {

    /**
     * Find by customer ID
     */
    Optional<CustomerAnalytics> findByCustomerId(Long customerId);

    /**
     * Find top customers by revenue
     */
    @Query(value = "{}", sort = "{ 'totalRevenue': -1 }")
    List<CustomerAnalytics> findTopCustomersByRevenue();

    /**
     * Find top customers by order count
     */
    @Query(value = "{}", sort = "{ 'totalOrders': -1 }")
    List<CustomerAnalytics> findTopCustomersByOrderCount();

    /**
     * Find customers with revenue above threshold
     */
    List<CustomerAnalytics> findByTotalRevenueGreaterThan(BigDecimal threshold);

    /**
     * Find customers with order count above threshold
     */
    List<CustomerAnalytics> findByTotalOrdersGreaterThan(Integer threshold);
}