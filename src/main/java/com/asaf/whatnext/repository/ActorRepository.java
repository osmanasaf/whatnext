package com.asaf.whatnext.repository;

import com.asaf.whatnext.models.Actor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActorRepository extends JpaRepository<Actor, Long> {
    Actor findByName(String name);
    
    List<Actor> findByNameContainingIgnoreCase(String name);

    @Query("SELECT a FROM Actor a LEFT JOIN a.events e GROUP BY a ORDER BY COUNT(e) DESC")
    List<Actor> findTopActorsByEventCount(Pageable pageable);
}


