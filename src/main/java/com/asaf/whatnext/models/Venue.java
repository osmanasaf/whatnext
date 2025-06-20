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
public class Venue extends BaseEntity {
    private String name;
    private String city;
    private String location;
    private int capacity;
    
    @OneToMany(mappedBy = "venue")
    @JsonManagedReference(value = "venue-events")
    private List<ConcertEvent> events;
}
