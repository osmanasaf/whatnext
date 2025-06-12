package com.asaf.whatnext.models;

import java.lang.String;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.JoinColumn;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import jakarta.persistence.*;

@Data
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@EqualsAndHashCode(callSuper = true)
public class Event extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EventSourceType source;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EventType type;

    @ElementCollection
    @CollectionTable(name = "event_ticket_urls", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "ticket_url")
    private List<String> ticketUrls = new ArrayList<>();

    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "venue_id")
    private Venue venue;

    public void setVenue(Venue venue) {
        this.venue = venue;
    }

    public void addTicketUrl(String url) {
        if (!ticketUrls.contains(url)) {
            ticketUrls.add(url);
        }
    }
}
