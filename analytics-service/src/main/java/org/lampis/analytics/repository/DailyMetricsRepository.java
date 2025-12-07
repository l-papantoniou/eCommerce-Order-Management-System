package org.lampis.analytics.repository;

import org.lampis.analytics.model.DailyMetrics;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for DailyMetrics MongoDB operations
 */
@Repository
public interface DailyMetricsRepository extends MongoRepository<DailyMetrics, String> {

    /**
     * Find by specific date
     */
    Optional<DailyMetrics> findByDate(LocalDate date);

    /**
     * Find metrics within date range
     */
    List<DailyMetrics> findByDateBetweenOrderByDateAsc(LocalDate startDate, LocalDate endDate);

    /**
     * Find metrics after a specific date
     */
    List<DailyMetrics> findByDateAfterOrderByDateAsc(LocalDate date);

    /**
     * Find last N days
     */
    List<DailyMetrics> findTop30ByOrderByDateDesc();
}