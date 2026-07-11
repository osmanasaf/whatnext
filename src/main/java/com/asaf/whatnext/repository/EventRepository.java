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

    @Query("SELECT DISTINCT e FROM Event e LEFT JOIN FETCH e.venue LEFT JOIN FETCH e.eventImage "
            + "WHERE e.startDate BETWEEN :start AND :end AND " + EventRepositoryCityMatch.JPQL_EQ_ISTANBUL)
    List<Event> findByStartDateBetweenAndIstanbul(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT e FROM Event e LEFT JOIN FETCH e.venue LEFT JOIN FETCH e.eventImage "
            + "WHERE e.startDate BETWEEN :start AND :end AND e.type = :type AND " + EventRepositoryCityMatch.JPQL_EQ_ISTANBUL)
    List<Event> findByStartDateBetweenAndTypeAndIstanbul(@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("type") EventType type);

    /** Gün o etkinliğin kapsamındayken: tek günlük kayıtta endDate null ise sadece startDate günü aktif sayılır. */
    @Query("SELECT DISTINCT e FROM Event e LEFT JOIN FETCH e.venue LEFT JOIN FETCH e.eventImage "
            + "WHERE e.type = :type AND " + EventRepositoryCityMatch.JPQL_EQ_ISTANBUL
            + " AND (e.startDate = :d OR (e.endDate IS NOT NULL AND e.startDate <= :d AND e.endDate >= :d)) "
            + "ORDER BY e.startDate ASC, e.id ASC")
    List<Event> findByTypeAndIstanbulActiveOnDate(@Param("type") EventType type, @Param("d") LocalDate d);

    @Query("SELECT e FROM Event e LEFT JOIN FETCH e.venue LEFT JOIN FETCH e.eventImage "
            + "WHERE e.type = :type AND " + EventRepositoryCityMatch.JPQL_EQ_PARAM
            + " AND (e.startDate = :d OR (e.endDate IS NOT NULL AND e.startDate <= :d AND e.endDate >= :d)) "
            + "ORDER BY e.startDate ASC, e.id ASC")
    List<Event> findByTypeAndCityActiveOnDate(@Param("type") EventType type, @Param("city") String city, @Param("d") LocalDate d);

    List<Event> findByType(EventType type);

    List<Event> findBySource(EventSourceType source);

    List<Event> findByVenue(Venue venue);

    List<Event> findByStartDate(LocalDate startDate);

    @Query("SELECT e FROM Event e WHERE " + EventRepositoryCityMatch.JPQL_EQ_PARAM)
    List<Event> findByCity(@Param("city") String venueCity);

    List<Event> findByStartDateBetween(LocalDate startDate, LocalDate endDate);

    List<Event> findByStartDateBetweenAndType(LocalDate startDate, LocalDate endDate, EventType type);

    @Query("SELECT e FROM Event e WHERE e.startDate BETWEEN :start AND :end AND " + EventRepositoryCityMatch.JPQL_EQ_PARAM)
    List<Event> findByStartDateBetweenAndCity(@Param("start") LocalDate startDate, @Param("end") LocalDate endDate, @Param("city") String city);

    @Query("SELECT e FROM Event e WHERE e.startDate BETWEEN :start AND :end AND e.type = :type AND " + EventRepositoryCityMatch.JPQL_EQ_PARAM)
    List<Event> findByStartDateBetweenAndTypeAndCity(@Param("start") LocalDate startDate, @Param("end") LocalDate endDate,
            @Param("type") EventType type, @Param("city") String city);

    @Query("SELECT e FROM Event e WHERE e.startDate BETWEEN :start AND :end AND " + EventRepositoryCityMatch.JPQL_EQ_PARAM)
    List<Event> findByStartDateBetweenAndCityIgnoreCase(@Param("start") LocalDate startDate, @Param("end") LocalDate endDate, @Param("city") String city);

    @Query("SELECT e FROM Event e WHERE e.startDate BETWEEN :start AND :end AND e.type = :type AND " + EventRepositoryCityMatch.JPQL_EQ_PARAM)
    List<Event> findByStartDateBetweenAndTypeAndCityIgnoreCase(@Param("start") LocalDate startDate, @Param("end") LocalDate endDate,
            @Param("type") EventType type, @Param("city") String city);
}
