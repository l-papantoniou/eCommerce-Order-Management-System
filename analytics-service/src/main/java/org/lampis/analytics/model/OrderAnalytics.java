package org.lampis.analytics.model;

import org.lampis.common.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * MongoDB document for order analytics
 * Represents a denormalized view of order data optimized for reporting
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "order_analytics")
public class OrderAnalytics {

    @Id
    private String id;

    @Indexed
    private Long orderId;

    @Indexed
    private Long customerId;

    @Indexed
    private OrderStatus status;

    private BigDecimal totalAmount;

    @Indexed
    private LocalDateTime orderDate;

    private LocalDateTime processedDate;
    private LocalDateTime shippedDate;

    private Integer itemCount;
    private List<OrderLineItem> items;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderLineItem {
        private Long productId;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
    }

    /**
     * Update status with timestamp
     */
    public void updateStatus(OrderStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();

        switch (newStatus) {
            case PROCESSING -> this.processedDate = LocalDateTime.now();
            case SHIPPED -> this.shippedDate = LocalDateTime.now();
        }
    }
}