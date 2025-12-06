package org.lampis.common.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lampis.common.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for order information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long orderId;
    private Long customerId;
    private OrderStatus status;
    private LocalDateTime orderDate;
    private BigDecimal totalAmount;
    private List<OrderLineDTO> orderLines;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

