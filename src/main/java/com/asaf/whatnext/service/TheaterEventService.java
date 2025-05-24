package com.asaf.whatnext.service;

import com.asaf.whatnext.models.PerformingArt;
import com.asaf.whatnext.repository.TheaterEventRepository;
import com.asaf.whatnext.enums.PerformanceType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TheaterEventService extends BaseService<PerformingArt, Long> {

    public TheaterEventService(TheaterEventRepository repository) {
        super(repository);
    }

    public List<PerformingArt> findByPlaywright(String playwright) {
        return ((TheaterEventRepository) repository).findByPlaywright(playwright);
    }

    public List<PerformingArt> findByActor(Long actorId) {
        return ((TheaterEventRepository) repository).findByActorsId(actorId);
    }

    /**
     * Find a performing art event by title, start date, and performance type
     * Used for duplicate checking when saving events
     * 
     * @param title The title of the event
     * @param startDate The start date of the event
     * @param performanceType The performance type of the event
     * @return Optional containing the event if found, empty otherwise
     */
    public Optional<PerformingArt> findByTitleDateAndType(String title, LocalDate startDate, PerformanceType performanceType) {
        return ((TheaterEventRepository) repository).findByTitleAndStartDateAndPerformanceType(title, startDate, performanceType);
    }

    /**
     * Check if a performing art event with the given title, start date, and performance type exists
     * 
     * @param title The title of the event
     * @param startDate The start date of the event
     * @param performanceType The performance type of the event
     * @return true if an event exists, false otherwise
     */
    public boolean existsByTitleDateAndType(String title, LocalDate startDate, PerformanceType performanceType) {
        return ((TheaterEventRepository) repository).existsByTitleAndStartDateAndPerformanceType(title, startDate, performanceType);
    }
}
