package com.asaf.whatnext.repository;

import com.asaf.whatnext.models.Venue;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VenueRepository extends JpaRepository<Venue, Long> {
    Venue findByName(String name);

    Venue findBySlug(String slug);
    
    List<Venue> findByNameContainingIgnoreCase(String name);
    
    @Query("SELECT v FROM Venue v LEFT JOIN v.events e GROUP BY v ORDER BY COUNT(e) DESC")
    List<Venue> findTopVenuesByEventCount(Pageable pageable);
}
