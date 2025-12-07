package org.lampis.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lampis.common.enums.OrderStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MongoDB document for customer analytics
 * Aggregated customer statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "customer_analytics")
public class CustomerAnalytics {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long customerId;

    private Integer totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;

    private LocalDateTime firstOrderDate;
    private LocalDateTime lastOrderDate;

    private Integer ordersUnprocessed;
    private Integer ordersProcessing;
    private Integer ordersProcessed;
    private Integer ordersShipped;
    private Integer ordersCancelled;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Increment order count and update revenue
     */
    public void addOrder(BigDecimal orderAmount) {
        this.totalOrders = (this.totalOrders == null ? 0 : this.totalOrders) + 1;
        this.totalRevenue = (this.totalRevenue == null ? BigDecimal.ZERO : this.totalRevenue)
                .add(orderAmount);
        this.averageOrderValue = this.totalRevenue.divide(
                BigDecimal.valueOf(this.totalOrders),
                2,
                java.math.RoundingMode.HALF_UP
        );
        this.lastOrderDate = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Update status counts
     */
    public void incrementStatusCount(OrderStatus status) {
        switch (status) {
            case UNPROCESSED -> this.ordersUnprocessed =
                    (this.ordersUnprocessed == null ? 0 : this.ordersUnprocessed) + 1;
            case PROCESSING -> this.ordersProcessing =
                    (this.ordersProcessing == null ? 0 : this.ordersProcessing) + 1;
            case PROCESSED -> this.ordersProcessed =
                    (this.ordersProcessed == null ? 0 : this.ordersProcessed) + 1;
            case SHIPPED -> this.ordersShipped =
                    (this.ordersShipped == null ? 0 : this.ordersShipped) + 1;
            case CANCELLED -> this.ordersCancelled =
                    (this.ordersCancelled == null ? 0 : this.ordersCancelled) + 1;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Decrement status count (for status changes)
     */
    public void decrementStatusCount(OrderStatus status) {
        switch (status) {
            case UNPROCESSED -> this.ordersUnprocessed =
                    Math.max(0, (this.ordersUnprocessed == null ? 0 : this.ordersUnprocessed) - 1);
            case PROCESSING -> this.ordersProcessing =
                    Math.max(0, (this.ordersProcessing == null ? 0 : this.ordersProcessing) - 1);
            case PROCESSED -> this.ordersProcessed =
                    Math.max(0, (this.ordersProcessed == null ? 0 : this.ordersProcessed) - 1);
            case SHIPPED -> this.ordersShipped =
                    Math.max(0, (this.ordersShipped == null ? 0 : this.ordersShipped) - 1);
            case CANCELLED -> this.ordersCancelled =
                    Math.max(0, (this.ordersCancelled == null ? 0 : this.ordersCancelled) - 1);
        }
        this.updatedAt = LocalDateTime.now();
    }
}