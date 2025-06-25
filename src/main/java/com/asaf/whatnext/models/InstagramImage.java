package com.asaf.whatnext.models;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
public class InstagramImage extends BaseEntity {

    private String cloudinaryUrl;
    private String cloudinaryPublicId;
    private String postType; // e.g., "POST", "STORY", "COVER"
    private String eventType; // e.g., "CONCERT", "THEATER", "SMART_STORY"
    private LocalDate creationDate;
    private boolean postedToInstagram = false;
} 