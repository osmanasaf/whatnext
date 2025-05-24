package com.asaf.whatnext.models;

import com.asaf.whatnext.enums.ConcertType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class ConcertEvent extends Event {
    @ManyToOne
    @JoinColumn(name = "artist_id")
    private Artist artist;
    
    @ManyToOne
    @JoinColumn(name = "venue_id")
    private Venue venue;
    
    private ConcertType concertType;
}
