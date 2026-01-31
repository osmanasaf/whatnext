package com.asaf.whatnext.repository;

import com.asaf.whatnext.models.InstagramImage;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InstagramImageRepository extends BaseRepository<InstagramImage, Long> {
    Optional<InstagramImage> findByPostTypeAndEventTypeAndCreationDate(String postType, String eventType, LocalDate creationDate);
    List<InstagramImage> findByCreationDateAndPostedToInstagramFalse(LocalDate date);

    boolean existsByCreationDateAndPostTypeAndPostedToInstagramIsTrue(LocalDate date, String postType);
} 