package com.asaf.whatnext.dto;

import com.asaf.whatnext.enums.EventType;
import lombok.Data;
import java.time.LocalDate;

@Data
public class EventSummaryDTO {
    private Long id;
    private String title;
    private LocalDate startDate;
    private EventType type;
    private String city;
    private String venueName;
    private String imageUrl;
} 