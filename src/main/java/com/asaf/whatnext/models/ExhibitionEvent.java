package com.asaf.whatnext.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class ExhibitionEvent extends Event {
    @ManyToOne
    @JoinColumn(name = "artist_id")
    @JsonBackReference(value = "artist-exhibitions")
    private Artist artist;
    
    @ManyToOne
    @JoinColumn(name = "gallery_id")
    @JsonBackReference(value = "gallery-events")
    private Gallery gallery;
    
    private String artType;
}
