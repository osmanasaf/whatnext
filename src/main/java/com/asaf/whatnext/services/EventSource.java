package com.asaf.whatnext.services;

import com.asaf.whatnext.models.Event;
import java.util.List;

public interface EventSource {
    List<Event> fetchEvents();
    List<Event> fetchEvents(String city);
    List<Event> fetchEvents(String city, java.time.LocalDate startDate, java.time.LocalDate endDate);
    List<Event> fetchEvents(java.time.LocalDate startDate, java.time.LocalDate endDate);
    String getSourceName();
    boolean isAvailable();
    String getSourceUrl();
} 