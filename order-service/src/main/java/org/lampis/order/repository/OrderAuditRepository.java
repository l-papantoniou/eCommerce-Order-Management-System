package org.lampis.order.repository;

import org.lampis.order.entity.OrderAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for OrderAudit entity
 */
@Repository
public interface OrderAuditRepository extends JpaRepository<OrderAudit, Long> {

    /**
     * Find all audit entries for an order
     */
    List<OrderAudit> findByOrderIdOrderByChangedAtDesc(Long orderId);
}
