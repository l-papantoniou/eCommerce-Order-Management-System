package org.lampis.order.scheduler;

import org.lampis.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task for automatic order status progression
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusScheduler {

    private final OrderService orderService;

    /**
     * Progress order statuses every 5 minutes
     * Can be configured via application.properties
     */
    @Scheduled(fixedDelayString = "${order.status.progression.interval:300000}") // Default: 5 minutes
    public void progressOrderStatuses() {
        log.info("Executing scheduled order status progression task");
        try {
            orderService.progressOrderStatuses();
            log.info("Order status progression task completed successfully");
        } catch (Exception e) {
            log.error("Error during order status progression", e);
        }
    }
}
