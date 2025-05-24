package com.asaf.whatnext.repository;

import com.asaf.whatnext.models.PerformingArt;
import com.asaf.whatnext.enums.PerformanceType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TheaterEventRepository extends BaseRepository<PerformingArt, Long> {
    List<PerformingArt> findByPlaywright(String playwright);
    List<PerformingArt> findByActorsId(Long actorId);

    /**
     * Find a performing art event by title, start date, and performance type
     * Used for duplicate checking when saving events
     * 
     * @param title The title of the event
     * @param startDate The start date of the event
     * @param performanceType The performance type of the event
     * @return Optional containing the event if found, empty otherwise
     */
    Optional<PerformingArt> findByTitleAndStartDateAndPerformanceType(String title, LocalDate startDate, PerformanceType performanceType);

    /**
     * Check if a performing art event with the given title, start date, and performance type exists
     * 
     * @param title The title of the event
     * @param startDate The start date of the event
     * @param performanceType The performance type of the event
     * @return true if an event exists, false otherwise
     */
    boolean existsByTitleAndStartDateAndPerformanceType(String title, LocalDate startDate, PerformanceType performanceType);
}
