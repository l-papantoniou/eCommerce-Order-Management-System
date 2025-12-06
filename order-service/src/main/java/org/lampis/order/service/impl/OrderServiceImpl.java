package org.lampis.order.service.impl;

import org.lampis.common.dto.order.*;
import org.lampis.common.enums.OrderStatus;
import org.lampis.common.event.order.OrderCreatedEvent;
import org.lampis.common.event.order.OrderStatusChangedEvent;
import org.lampis.common.event.order.OrderUpdatedEvent;
import org.lampis.common.exception.InsufficientStockException;
import org.lampis.common.exception.InvalidOrderStateException;
import org.lampis.common.exception.ResourceNotFoundException;
import org.lampis.order.entity.Inventory;
import org.lampis.order.entity.Order;
import org.lampis.order.entity.OrderAudit;
import org.lampis.order.entity.OrderLine;
import org.lampis.order.repository.InventoryRepository;
import org.lampis.order.repository.OrderAuditRepository;
import org.lampis.order.repository.OrderRepository;
import org.lampis.order.service.EventPublisherService;
import org.lampis.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of OrderService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderAuditRepository orderAuditRepository;
    private final EventPublisherService eventPublisher;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());

        // Validate inventory
        validateInventory(request.getOrderLines());

        // Create order entity
        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .status(OrderStatus.UNPROCESSED)
                .orderDate(LocalDateTime.now())
                .totalAmount(BigDecimal.ZERO)
                .build();

        // Add order lines
        for (OrderLineDTO lineDTO : request.getOrderLines()) {
            OrderLine orderLine = OrderLine.builder()
                    .productId(lineDTO.getProductId())
                    .quantity(lineDTO.getQuantity())
                    .unitPrice(lineDTO.getUnitPrice())
                    .build();
            order.addOrderLine(orderLine);
        }

        // Calculate total
        order.calculateTotalAmount();

        // Reserve inventory
        reserveInventory(request.getOrderLines());

        // Save order
        order = orderRepository.save(order);
        log.info("Order created with ID: {}", order.getId());

        // Publish event
        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getOrderDate()
        );
        eventPublisher.publishOrderCreatedEvent(event);

        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        log.info("Fetching order with ID: {}", orderId);
        Order order = orderRepository.findByIdAndNotDeleted(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        log.info("Fetching all orders, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return orderRepository.findAllNotDeleted(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByCustomerId(Long customerId, Pageable pageable) {
        log.info("Fetching orders for customer: {}", customerId);
        return orderRepository.findByCustomerId(customerId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        log.info("Fetching orders with status: {}", status);
        return orderRepository.findByStatus(status, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public OrderResponse updateOrder(Long orderId, UpdateOrderRequest request) {
        log.info("Updating order: {}", orderId);

        Order order = orderRepository.findByIdAndNotDeleted(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Can only update UNPROCESSED orders
        if (order.getStatus() != OrderStatus.UNPROCESSED) {
            throw new InvalidOrderStateException("Cannot update order in " + order.getStatus() + " status");
        }

        // Release old inventory
        releaseInventory(order.getOrderLines());

        // Validate new inventory
        validateInventory(request.getOrderLines());

        // Clear existing order lines
        order.getOrderLines().clear();

        // Add new order lines
        for (OrderLineDTO lineDTO : request.getOrderLines()) {
            OrderLine orderLine = OrderLine.builder()
                    .productId(lineDTO.getProductId())
                    .quantity(lineDTO.getQuantity())
                    .unitPrice(lineDTO.getUnitPrice())
                    .build();
            order.addOrderLine(orderLine);
        }

        // Recalculate total
        order.calculateTotalAmount();

        // Reserve new inventory
        reserveInventory(request.getOrderLines());

        // Save
        order = orderRepository.save(order);
        log.info("Order updated: {}", orderId);

        // Create audit trail
        createAuditEntry(orderId, "ORDER_LINES", "updated", "updated");

        // Publish event
        OrderUpdatedEvent event = new OrderUpdatedEvent(
                order.getId(),
                order.getCustomerId(),
                order.getTotalAmount()
        );
        eventPublisher.publishOrderUpdatedEvent(event);

        return mapToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
        log.info("Updating order {} status to: {}", orderId, newStatus);

        Order order = orderRepository.findByIdAndNotDeleted(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        OrderStatus oldStatus = order.getStatus();

        // Validate state transition
        if (!oldStatus.canTransitionTo(newStatus)) {
            throw new InvalidOrderStateException(oldStatus, newStatus);
        }

        // Update status
        order.setStatus(newStatus);
        order = orderRepository.save(order);
        log.info("Order {} status changed from {} to {}", orderId, oldStatus, newStatus);

        // Create audit trail
        createAuditEntry(orderId, "STATUS", oldStatus.name(), newStatus.name());

        // Publish event
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                order.getId(),
                order.getCustomerId(),
                oldStatus,
                newStatus
        );
        eventPublisher.publishOrderStatusChangedEvent(event);

        return mapToResponse(order);
    }

    @Override
    @Transactional
    public void deleteOrder(Long orderId) {
        log.info("Deleting order: {}", orderId);

        Order order = orderRepository.findByIdAndNotDeleted(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Release inventory if order is UNPROCESSED
        if (order.getStatus() == OrderStatus.UNPROCESSED) {
            releaseInventory(order.getOrderLines());
        }

        // Soft delete
        order.softDelete();
        orderRepository.save(order);

        // Create audit trail
        createAuditEntry(orderId, "DELETED", "false", "true");

        log.info("Order {} deleted", orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderAuditResponse> getOrderHistory(Long orderId) {
        log.info("Fetching audit history for order: {}", orderId);

        // Verify order exists
        orderRepository.findByIdAndNotDeleted(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        return orderAuditRepository.findByOrderIdOrderByChangedAtDesc(orderId)
                .stream()
                .map(this::mapToAuditResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void progressOrderStatuses() {
        log.info("Starting automatic order status progression");

        // Progress UNPROCESSED to PROCESSING
        List<Order> unprocessedOrders = orderRepository.findByStatusForProcessing(OrderStatus.UNPROCESSED);
        for (Order order : unprocessedOrders) {
            updateOrderStatus(order.getId(), OrderStatus.PROCESSING);
        }

        // Progress PROCESSING to PROCESSED
        List<Order> processingOrders = orderRepository.findByStatusForProcessing(OrderStatus.PROCESSING);
        for (Order order : processingOrders) {
            updateOrderStatus(order.getId(), OrderStatus.PROCESSED);
        }

        // Progress PROCESSED to SHIPPED
        List<Order> processedOrders = orderRepository.findByStatusForProcessing(OrderStatus.PROCESSED);
        for (Order order : processedOrders) {
            updateOrderStatus(order.getId(), OrderStatus.SHIPPED);
        }

        log.info("Order status progression completed");
    }

    // Helper methods

    private void validateInventory(List<OrderLineDTO> orderLines) {
        for (OrderLineDTO line : orderLines) {
            Inventory inventory = inventoryRepository.findByProductId(line.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", line.getProductId()));

            if (!inventory.hasSufficientStock(line.getQuantity())) {
                throw new InsufficientStockException(
                        line.getProductId(),
                        line.getQuantity(),
                        inventory.getAvailableStock()
                );
            }
        }
    }

    private void reserveInventory(List<OrderLineDTO> orderLines) {
        for (OrderLineDTO line : orderLines) {
            Inventory inventory = inventoryRepository.findByProductIdWithLock(line.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", line.getProductId()));

            inventory.reserveStock(line.getQuantity());
            inventoryRepository.save(inventory);
        }
    }

    private void releaseInventory(List<OrderLine> orderLines) {
        for (OrderLine line : orderLines) {
            Inventory inventory = inventoryRepository.findByProductIdWithLock(line.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", line.getProductId()));

            inventory.releaseStock(line.getQuantity());
            inventoryRepository.save(inventory);
        }
    }

    private void createAuditEntry(Long orderId, String fieldName, String oldValue, String newValue) {
        OrderAudit audit = OrderAudit.builder()
                .orderId(orderId)
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();
        orderAuditRepository.save(audit);
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderLineDTO> lineDTOs = order.getOrderLines().stream()
                .map(line -> OrderLineDTO.builder()
                        .id(line.getId())
                        .productId(line.getProductId())
                        .quantity(line.getQuantity())
                        .unitPrice(line.getUnitPrice())
                        .lineTotal(line.getLineTotal())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .status(order.getStatus())
                .orderDate(order.getOrderDate())
                .totalAmount(order.getTotalAmount())
                .orderLines(lineDTOs)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderAuditResponse mapToAuditResponse(OrderAudit audit) {
        return OrderAuditResponse.builder()
                .id(audit.getId())
                .orderId(audit.getOrderId())
                .fieldName(audit.getFieldName())
                .oldValue(audit.getOldValue())
                .newValue(audit.getNewValue())
                .changedAt(audit.getChangedAt())
                .changedBy(audit.getChangedBy())
                .build();
    }
}