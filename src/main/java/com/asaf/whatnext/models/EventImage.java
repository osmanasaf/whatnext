package com.asaf.whatnext.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "event_images")
public class EventImage extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String imageData;

    @Column
    private String imageType;

    @Column
    private String imageSourceUrl;

    @OneToOne
    @JoinColumn(name = "event_id")
    private Event event;
} 