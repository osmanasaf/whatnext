package com.asaf.whatnext.scraper;

import com.asaf.whatnext.models.Event;
import java.time.LocalDate;
import java.util.List;

public interface EventSource {
    List<Event> fetchEvents();
    List<Event> fetchEvents(String city);
    List<Event> fetchEvents(String city, LocalDate startDate, LocalDate endDate);
    List<Event> fetchEvents(LocalDate startDate, LocalDate endDate);
    String getSourceName();
    boolean isAvailable();
    String getSourceUrl();
} 