package com.asaf.whatnext.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "instagram_image", indexes = {
    @Index(name = "idx_instagram_image_creation_posttype_posted", columnList = "creationDate,postType,postedToInstagram")
})
@Data
public class InstagramImage extends BaseEntity {

    private String cloudinaryUrl;
    private String cloudinaryPublicId;
    private String postType;
    private String eventType;
    private LocalDate creationDate;
    private boolean postedToInstagram = false;
} 