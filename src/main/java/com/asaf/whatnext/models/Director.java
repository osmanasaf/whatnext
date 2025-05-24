package com.asaf.whatnext.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import java.util.List;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class Director extends BaseEntity {
    private String name;
    private String biography;
    private String birthDate;
    
    @OneToMany(mappedBy = "director")
    private List<PerformingArt> events;
}
