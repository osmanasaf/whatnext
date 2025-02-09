package com.asaf.whatnext.repository;

import com.asaf.whatnext.models.Artist;

public interface ArtistRepository extends BaseRepository<Artist, Long> {
    Artist findByName(String name);

}
