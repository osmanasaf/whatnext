package com.asaf.whatnext.repository;

import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.enums.City;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.models.Venue;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventRepository extends BaseRepository<Event, Long> {

    List<Event> findByType(EventType type);

    List<Event> findBySource(EventSourceType source);

    List<Event> findByVenue(Venue venue);

    List<Event> findByStartDate(LocalDate startDate);

    List<Event> findByCity(String venue_city);

    List<Event> findByStartDateBetween(LocalDate startDate, LocalDate endDate);

    List<Event> findByStartDateBetweenAndType(LocalDate startDate, LocalDate endDate, EventType type);

    List<Event> findByStartDateBetweenAndCity(LocalDate startDate, LocalDate endDate, String city);

    List<Event> findByStartDateBetweenAndTypeAndCity(LocalDate startDate, LocalDate endDate, EventType type, String city);
}
