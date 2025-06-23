package com.asaf.whatnext.dto;

import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.enums.PerformanceType;
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
    private PerformanceType performanceType;
} 