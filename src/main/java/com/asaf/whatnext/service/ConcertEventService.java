package com.asaf.whatnext.service;

import com.asaf.whatnext.models.ConcertEvent;
import com.asaf.whatnext.repository.ConcertEventRepository;
import com.asaf.whatnext.enums.EventType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ConcertEventService extends BaseService<ConcertEvent, Long> {

    public ConcertEventService(ConcertEventRepository repository) {
        super(repository);
    }

    public List<ConcertEvent> findByArtist(Long artistId) {
        return ((ConcertEventRepository) repository).findByArtistId(artistId);
    }

    public List<ConcertEvent> findByVenue(Long venueId) {
        return ((ConcertEventRepository) repository).findByVenueId(venueId);
    }

    public List<ConcertEvent> findByDateAndVenue(LocalDate startDate, String venueName) {
        return ((ConcertEventRepository) repository).findByStartDateAndVenue_Name(startDate, venueName);
    }

    public List<ConcertEvent> findByDateAndVenueAndTitleOrArtist(LocalDate startDate, String venueName, String title, String artistName) {
        return ((ConcertEventRepository) repository).findByStartDateAndVenue_NameAndTitleContainingOrArtist_Name(
            startDate, venueName, title, artistName);
    }

    public boolean existsByArtistNameAndDate(String artistName, LocalDate startDate) {
        return ((ConcertEventRepository) repository).existsByArtist_NameAndStartDate(artistName, startDate);
    }

    public Optional<ConcertEvent> findByArtistNameAndDateAndVenueAndType(String artistName, LocalDate startDate, String venueName, EventType type) {
        return ((ConcertEventRepository) repository).findByArtist_NameAndStartDateAndVenue_NameAndType(artistName, startDate, venueName, type);
    }

    public boolean existsByArtistNameAndDateAndVenueAndType(String artistName, LocalDate startDate, String venueName, EventType type) {
        return ((ConcertEventRepository) repository).existsByArtist_NameAndStartDateAndVenue_NameAndType(artistName, startDate, venueName, type);
    }

    public Optional<ConcertEvent> findByTitleAndDateAndVenueAndType(String title, LocalDate startDate, String venueName, EventType type) {
        return ((ConcertEventRepository) repository).findByTitleAndStartDateAndVenue_NameAndType(title, startDate, venueName, type);
    }

    public boolean existsByTitleAndDateAndVenueAndType(String title, LocalDate startDate, String venueName, EventType type) {
        return ((ConcertEventRepository) repository).existsByTitleAndStartDateAndVenue_NameAndType(title, startDate, venueName, type);
    }

    public List<ConcertEvent> findByTitleContainingAndDateAndVenueAndType(String title, LocalDate startDate, String venueName, EventType type) {
        return ((ConcertEventRepository) repository).findByTitleContainingAndStartDateAndVenue_NameAndType(title, startDate, venueName, type);
    }
}
