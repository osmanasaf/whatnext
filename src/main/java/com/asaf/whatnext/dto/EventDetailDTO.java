package com.asaf.whatnext.dto;

import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class EventDetailDTO {
    private Long id;
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private EventType type;
    private EventSourceType source;
    private String city;
    private String venueName;
    private String venueLocation;
    private List<String> ticketUrls;
    private String imageUrl;
    private String imageSourceUrl;
    
    // İlişkili entity'lerin ID'leri
    private Long venueId;
    private Long artistId;
    private Long directorId;
    private Long galleryId;
} 