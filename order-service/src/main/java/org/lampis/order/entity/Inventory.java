package org.lampis.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Inventory entity for product stock management
 */
@Entity
@Table(name = "inventory")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "available_stock", nullable = false)
    private Integer availableStock;

    /**
     * Check if sufficient stock is available
     */
    public boolean hasSufficientStock(Integer requestedQuantity) {
        return availableStock >= requestedQuantity;
    }

    /**
     * Reserve stock for an order
     */
    public void reserveStock(Integer quantity) {
        this.availableStock -= quantity;
    }

    /**
     * Release reserved stock (e.g., order cancelled)
     */
    public void releaseStock(Integer quantity) {
        this.availableStock += quantity;
    }
}
