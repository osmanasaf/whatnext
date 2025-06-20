package com.asaf.whatnext.repository;

import com.asaf.whatnext.models.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VenueRepository extends JpaRepository<Venue, Long> {
    Venue findByName(String name);
}
