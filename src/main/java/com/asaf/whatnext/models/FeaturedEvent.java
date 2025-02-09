package com.asaf.whatnext.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "featured_events")
@Entity
public class FeaturedEvent extends BaseEntity{

    @Column(nullable = false)
    private Long eventId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private LocalDateTime featuredUntil;
}
