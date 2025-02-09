package com.asaf.whatnext.repository;

import com.asaf.whatnext.models.ConcertEvent;

import java.util.List;

public interface ConcertEventRepository extends BaseRepository<ConcertEvent, Long> {
    List<ConcertEvent> findByArtistId(Long artistId);
    List<ConcertEvent> findByVenueId(Long venueId);
}
