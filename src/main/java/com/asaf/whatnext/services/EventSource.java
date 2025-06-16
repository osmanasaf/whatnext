package com.asaf.whatnext.services;

import com.asaf.whatnext.models.Event;
import java.util.List;

public interface EventSource {
    List<Event> fetchEvents();
    List<Event> fetchEvents(String city);
    String getSourceName();
    boolean isAvailable();
    String getSourceUrl();
} 