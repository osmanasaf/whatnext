package com.asaf.whatnext.repository;

import com.asaf.whatnext.models.EventImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventImageRepository extends JpaRepository<EventImage, Long> {
    EventImage findByEventId(Long eventId);
} 