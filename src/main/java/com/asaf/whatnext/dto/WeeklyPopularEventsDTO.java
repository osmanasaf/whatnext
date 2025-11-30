package com.asaf.whatnext.dto;

import lombok.Data;
import java.util.List;

@Data
public class WeeklyPopularEventsDTO {
    private List<EventSummaryDTO> concerts;
    private List<EventSummaryDTO> theaters;
    private List<EventSummaryDTO> standups;
    private String weekRange; // "01 Aralık - 07 Aralık" gibi
}

