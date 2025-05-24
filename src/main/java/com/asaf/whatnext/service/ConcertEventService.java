package com.asaf.whatnext.service;

import com.asaf.whatnext.models.ConcertEvent;
import com.asaf.whatnext.repository.ConcertEventRepository;
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

    public boolean existsByArtistNameAndDate(String artistName, LocalDate startDate) {
        return ((ConcertEventRepository) repository).existsByArtist_NameAndStartDate(artistName, startDate);
    }
}
