package com.asaf.whatnext.models;

import com.asaf.whatnext.enums.PerformanceType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonBackReference;
import java.util.List;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class PerformingArt extends Event {
    private String playwright;

    @ManyToMany
    @JoinTable(
        name = "event_actors",
        joinColumns = @JoinColumn(name = "event_id"),
        inverseJoinColumns = @JoinColumn(name = "actor_id")
    )
    @JsonBackReference(value = "actor-events")
    private List<Actor> actors;

    @ManyToOne
    @JoinColumn(name = "director_id")
    @JsonBackReference(value = "director-events")
    private Director director;

    @Enumerated(EnumType.STRING)
    private PerformanceType performanceType;
}
