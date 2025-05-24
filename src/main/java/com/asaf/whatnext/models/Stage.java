package com.asaf.whatnext.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@Data
@Entity
public class Stage {
    @Id
    private Long id;
    private String name;
    private String location;
    private String description;
    private int seatingCapacity;

    @ManyToMany(mappedBy = "stages")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<PerformingArt> events;
}
