package com.asaf.whatnext.repository;

import com.asaf.whatnext.models.ConcertEvent;
import com.asaf.whatnext.enums.EventType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ConcertEventRepository extends BaseRepository<ConcertEvent, Long> {
    List<ConcertEvent> findByArtistId(Long artistId);
    List<ConcertEvent> findByVenueId(Long venueId);

    /**
     * Find concert events by date and venue, then filter by title containing or artist name
     * Used for duplicate checking when saving events
     * 
     * @param startDate The start date of the event
     * @param venueName The name of the venue
     * @param title The title to check for containing
     * @param artistName The artist name to check
     * @return List of events that match the criteria
     */
    List<ConcertEvent> findByStartDateAndVenue_NameAndTitleContainingOrArtist_Name(
        LocalDate startDate, 
        String venueName, 
        String title, 
        String artistName
    );

    /**
     * Find concert events by date and venue
     * Used for duplicate checking when saving events
     * 
     * @param startDate The start date of the event
     * @param venueName The name of the venue
     * @return List of events that match the criteria
     */
    List<ConcertEvent> findByStartDateAndVenue_Name(LocalDate startDate, String venueName);

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

    /**
     * Find a concert event by artist name, start date, venue name and type
     * Used for duplicate checking when saving events
     * 
     * @param artistName The name of the artist
     * @param startDate The start date of the event
     * @param venueName The name of the venue
     * @param type The type of the event
     * @return Optional containing the event if found, empty otherwise
     */
    Optional<ConcertEvent> findByArtist_NameAndStartDateAndVenue_NameAndType(String artistName, LocalDate startDate, String venueName, EventType type);

    /**
     * Check if a concert event with the given artist name, start date, venue name and type exists
     * 
     * @param artistName The name of the artist
     * @param startDate The start date of the event
     * @param venueName The name of the venue
     * @param type The type of the event
     * @return true if an event exists, false otherwise
     */
    boolean existsByArtist_NameAndStartDateAndVenue_NameAndType(String artistName, LocalDate startDate, String venueName, EventType type);

    /**
     * Find a concert event by title, start date, venue name and type
     * Used for duplicate checking when saving events
     * 
     * @param title The title of the event
     * @param startDate The start date of the event
     * @param venueName The name of the venue
     * @param type The type of the event
     * @return Optional containing the event if found, empty otherwise
     */
    Optional<ConcertEvent> findByTitleAndStartDateAndVenue_NameAndType(String title, LocalDate startDate, String venueName, EventType type);

    /**
     * Check if a concert event with the given title, start date, venue name and type exists
     * 
     * @param title The title of the event
     * @param startDate The start date of the event
     * @param venueName The name of the venue
     * @param type The type of the event
     * @return true if an event exists, false otherwise
     */
    boolean existsByTitleAndStartDateAndVenue_NameAndType(String title, LocalDate startDate, String venueName, EventType type);

    /**
     * Find a concert event by title containing, start date, venue name and type
     * Used for duplicate checking when saving events
     * 
     * @param title The title of the event
     * @param startDate The start date of the event
     * @param venueName The name of the venue
     * @param type The type of the event
     * @return List of events that match the criteria
     */
    List<ConcertEvent> findByTitleContainingAndStartDateAndVenue_NameAndType(String title, LocalDate startDate, String venueName, EventType type);
}
