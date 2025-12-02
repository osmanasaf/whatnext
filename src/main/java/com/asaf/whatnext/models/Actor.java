package com.asaf.whatnext.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import java.util.List;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class Actor extends BaseEntity {
    private String name;
    private String biography;
    private String birthDate;
    
    @jakarta.persistence.Column(name = "popularity_score", columnDefinition = "integer default 0")
    private int popularityScore = 0;
    
    @ManyToMany(mappedBy = "actors")
    @JsonManagedReference(value = "actor-events")
    private List<PerformingArt> events;
}
