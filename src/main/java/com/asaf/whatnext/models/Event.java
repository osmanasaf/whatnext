package com.asaf.whatnext.models;

import java.lang.String;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import com.asaf.whatnext.enums.EventSourceType;

@Data
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@EqualsAndHashCode(callSuper = true)
public class Event extends BaseEntity {
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String location;
    private String imageUrl;
    private String ticketUrl;
    private double price;
    private String category;

    @Enumerated(EnumType.STRING)
    private EventSourceType source;
}
