package org.lampis.common.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for order audit trail
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderAuditResponse {

    private Long id;
    private Long orderId;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private LocalDateTime changedAt;
    private String changedBy;
}
