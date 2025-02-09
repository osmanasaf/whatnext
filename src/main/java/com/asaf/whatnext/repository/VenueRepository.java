package com.asaf.whatnext.repository;

import com.asaf.whatnext.models.Venue;

public interface VenueRepository extends BaseRepository<Venue, Long> {
    Venue findByName(String name);
}
