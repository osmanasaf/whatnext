package com.asaf.whatnext.repository;

import com.asaf.whatnext.models.ConcertEvent;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ConcertEventRepository extends BaseRepository<ConcertEvent, Long> {
    List<ConcertEvent> findByArtistId(Long artistId);
    List<ConcertEvent> findByVenueId(Long venueId);

    /**
     * Find a concert event by artist name and start date
     * Used for duplicate checking when saving events
     * 
     * @param artistName The name of the artist
     * @param startDate The start date of the event
     * @return Optional containing the event if found, empty otherwise
     */
    Optional<ConcertEvent> findByArtist_NameAndStartDate(String artistName, LocalDate startDate);

    /**
     * Check if a concert event with the given artist name and start date exists
     * 
     * @param artistName The name of the artist
     * @param startDate The start date of the event
     * @return true if an event exists, false otherwise
     */
    boolean existsByArtist_NameAndStartDate(String artistName, LocalDate startDate);
}
