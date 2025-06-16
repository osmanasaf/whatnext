package com.asaf.whatnext.repository;

import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.enums.City;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.models.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByType(EventType type);
    List<Event> findBySource(EventSourceType source);
    List<Event> findByVenue(Venue venue);
    List<Event> findByStartDate(LocalDate startDate);
    List<Event> findByVenue_LocationContaining(String location);
    List<Event> findByStartDateBetween(LocalDate startDate, LocalDate endDate);
    List<Event> findByStartDateBetweenAndType(LocalDate startDate, LocalDate endDate, EventType type);
    List<Event> findByStartDateBetweenAndVenue_LocationContaining(LocalDate startDate, LocalDate endDate, String location);
    List<Event> findByStartDateBetweenAndTypeAndVenue_LocationContaining(LocalDate startDate, LocalDate endDate, EventType type, String location);
} 