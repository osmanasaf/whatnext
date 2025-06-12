package com.asaf.whatnext.repository;

import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
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
    List<Event> findByVenueId(Long venueId);
    List<Event> findByStartDate(LocalDate startDate);
    List<Event> findByStartDateAndVenue(LocalDate startDate, Venue venue);
} 