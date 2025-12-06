package org.lampis.order.repository;


import org.lampis.common.enums.OrderStatus;
import org.lampis.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Order entity
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Find order by ID excluding soft deleted
     */
    @Query("SELECT o FROM Order o WHERE o.id = :id AND o.deleted = false")
    Optional<Order> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * Find all non-deleted orders
     */
    @Query("SELECT o FROM Order o WHERE o.deleted = false")
    Page<Order> findAllNotDeleted(Pageable pageable);

    /**
     * Find orders by customer ID
     */
    @Query("SELECT o FROM Order o WHERE o.customerId = :customerId AND o.deleted = false")
    Page<Order> findByCustomerId(@Param("customerId") Long customerId, Pageable pageable);

    /**
     * Find orders by status
     */
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.deleted = false")
    Page<Order> findByStatus(@Param("status") OrderStatus status, Pageable pageable);

    /**
     * Find orders by status for automatic progression
     */
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.deleted = false")
    List<Order> findByStatusForProcessing(@Param("status") OrderStatus status);

    /**
     * Count orders by status
     */
    long countByStatusAndDeletedFalse(OrderStatus status);
}
