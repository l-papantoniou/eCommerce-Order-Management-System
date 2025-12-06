package org.lampis.common.exception;

import lombok.Getter;

/**
 * Exception thrown when there is insufficient stock for an order
 */
@Getter
public class InsufficientStockException extends BusinessException {

    private final Long productId;
    private final Integer requestedQuantity;
    private final Integer availableStock;

    public InsufficientStockException(Long productId, Integer requestedQuantity, Integer availableStock) {
        super("INSUFFICIENT_STOCK",
                String.format("Insufficient stock for product %d. Requested: %d, Available: %d",
                        productId, requestedQuantity, availableStock));
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.availableStock = availableStock;
    }

}
