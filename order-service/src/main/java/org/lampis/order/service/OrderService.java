package org.lampis.order.service;

import org.lampis.common.dto.order.CreateOrderRequest;
import org.lampis.common.dto.order.OrderAuditResponse;
import org.lampis.common.dto.order.OrderResponse;
import org.lampis.common.dto.order.UpdateOrderRequest;
import org.lampis.common.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for order operations
 */
public interface OrderService {

    /**
     * Create a new order
     */
    OrderResponse createOrder(CreateOrderRequest request);

    /**
     * Get order by ID
     */
    OrderResponse getOrderById(Long orderId);

    /**
     * Get all orders with pagination
     */
    Page<OrderResponse> getAllOrders(Pageable pageable);

    /**
     * Get orders by customer ID
     */
    Page<OrderResponse> getOrdersByCustomerId(Long customerId, Pageable pageable);

    /**
     * Get orders by status
     */
    Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable);

    /**
     * Update order
     */
    OrderResponse updateOrder(Long orderId, UpdateOrderRequest request);

    /**
     * Update order status
     */
    OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus);

    /**
     * Delete order (soft delete)
     */
    void deleteOrder(Long orderId);

    /**
     * Get order audit history
     */
    List<OrderAuditResponse> getOrderHistory(Long orderId);

    /**
     * Progress orders to next status (for scheduled task)
     */
    void progressOrderStatuses();
}
