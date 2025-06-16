package com.asaf.whatnext.models;

import com.asaf.whatnext.enums.ConcertType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class ConcertEvent extends Event {
    @ManyToOne
    @JoinColumn(name = "artist_id")
    @JsonBackReference(value = "artist-events")
    private Artist artist;
    
    private ConcertType concertType;
}
