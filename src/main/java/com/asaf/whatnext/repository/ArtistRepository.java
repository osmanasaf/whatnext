package com.asaf.whatnext.repository;

import com.asaf.whatnext.models.Artist;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ArtistRepository extends BaseRepository<Artist, Long> {
    Artist findByName(String name);
    
    List<Artist> findByNameContainingIgnoreCase(String name);

    @Query("SELECT a FROM Artist a LEFT JOIN a.events e GROUP BY a ORDER BY COUNT(e) DESC")
    List<Artist> findTopArtistsByEventCount(Pageable pageable);
}
