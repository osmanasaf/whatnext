package com.asaf.whatnext.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import java.util.List;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class Artist extends BaseEntity {
    private String name;
    private String genre;
    private String biography;
    
    @OneToMany(mappedBy = "artist")
    private List<ConcertEvent> events;
}
