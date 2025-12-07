package org.lampis.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * MongoDB document for daily metrics
 * Time-series data for trend analysis
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "daily_metrics")
public class DailyMetrics {

    @Id
    private String id;

    @Indexed(unique = true)
    private LocalDate date;

    private Integer totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;

    private Integer ordersCreated;
    private Integer ordersProcessed;
    private Integer ordersShipped;
    private Integer ordersCancelled;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Add order to daily metrics
     */
    public void addOrder(BigDecimal orderAmount) {
        this.totalOrders = (this.totalOrders == null ? 0 : this.totalOrders) + 1;
        this.ordersCreated = (this.ordersCreated == null ? 0 : this.ordersCreated) + 1;
        this.totalRevenue = (this.totalRevenue == null ? BigDecimal.ZERO : this.totalRevenue)
                .add(orderAmount);

        if (this.totalOrders > 0) {
            this.averageOrderValue = this.totalRevenue.divide(
                    BigDecimal.valueOf(this.totalOrders),
                    2,
                    java.math.RoundingMode.HALF_UP
            );
        }

        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Increment processed orders
     */
    public void incrementProcessed() {
        this.ordersProcessed = (this.ordersProcessed == null ? 0 : this.ordersProcessed) + 1;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Increment shipped orders
     */
    public void incrementShipped() {
        this.ordersShipped = (this.ordersShipped == null ? 0 : this.ordersShipped) + 1;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Increment cancelled orders
     */
    public void incrementCancelled() {
        this.ordersCancelled = (this.ordersCancelled == null ? 0 : this.ordersCancelled) + 1;
        this.updatedAt = LocalDateTime.now();
    }
}