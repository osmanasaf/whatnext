package com.asaf.whatnext.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import java.util.List;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class Artist extends BaseEntity {
    private String name;
    private String genre;
    private String biography;
    
    @OneToMany(mappedBy = "artist")
    @JsonManagedReference(value = "artist-events")
    private List<ConcertEvent> events;
}
