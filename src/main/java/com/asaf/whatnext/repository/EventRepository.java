package com.asaf.whatnext.repository;

import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.models.Venue;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventRepository extends BaseRepository<Event, Long> {

    @Query("SELECT DISTINCT e FROM Event e LEFT JOIN FETCH e.venue LEFT JOIN FETCH e.eventImage WHERE e.startDate BETWEEN :start AND :end AND (LOWER(e.city) = 'istanbul' OR e.city = '\u0130stanbul')")
    List<Event> findByStartDateBetweenAndIstanbul(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT e FROM Event e LEFT JOIN FETCH e.venue LEFT JOIN FETCH e.eventImage WHERE e.startDate BETWEEN :start AND :end AND e.type = :type AND (LOWER(e.city) = 'istanbul' OR e.city = '\u0130stanbul')")
    List<Event> findByStartDateBetweenAndTypeAndIstanbul(@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("type") EventType type);

    /** Gün o etkinliğin kapsamındayken: çok günlük kayıtlar (startDate–endDate) bu günü de içerir. */
    @Query("SELECT DISTINCT e FROM Event e LEFT JOIN FETCH e.venue LEFT JOIN FETCH e.eventImage "
            + "WHERE e.type = :type AND (LOWER(e.city) = 'istanbul' OR e.city = '\u0130stanbul') "
            + "AND e.startDate <= :d AND (e.endDate IS NULL OR e.endDate >= :d) ORDER BY e.startDate ASC, e.id ASC")
    List<Event> findByTypeAndIstanbulActiveOnDate(@Param("type") EventType type, @Param("d") LocalDate d);

    @Query("SELECT e FROM Event e LEFT JOIN FETCH e.venue LEFT JOIN FETCH e.eventImage "
            + "WHERE e.type = :type AND LOWER(e.city) = LOWER(:city) "
            + "AND e.startDate <= :d AND (e.endDate IS NULL OR e.endDate >= :d) ORDER BY e.startDate ASC, e.id ASC")
    List<Event> findByTypeAndCityActiveOnDate(@Param("type") EventType type, @Param("city") String city, @Param("d") LocalDate d);

    List<Event> findByType(EventType type);

    List<Event> findBySource(EventSourceType source);

    List<Event> findByVenue(Venue venue);

    List<Event> findByStartDate(LocalDate startDate);

    List<Event> findByCity(String venue_city);

    List<Event> findByStartDateBetween(LocalDate startDate, LocalDate endDate);

    List<Event> findByStartDateBetweenAndType(LocalDate startDate, LocalDate endDate, EventType type);

    List<Event> findByStartDateBetweenAndCity(LocalDate startDate, LocalDate endDate, String city);

    List<Event> findByStartDateBetweenAndTypeAndCity(LocalDate startDate, LocalDate endDate, EventType type, String city);

    List<Event> findByStartDateBetweenAndCityIgnoreCase(LocalDate startDate, LocalDate endDate, String city);

    List<Event> findByStartDateBetweenAndTypeAndCityIgnoreCase(LocalDate startDate, LocalDate endDate, EventType type, String city);
}
