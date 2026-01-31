package com.asaf.whatnext.scraper;

import java.time.LocalDate;
import java.util.List;

public interface EventSource {
    
    List<ScrapedEventData> scrapeEvents();
    
    List<ScrapedEventData> scrapeEvents(String city);
    
    List<ScrapedEventData> scrapeEvents(String city, LocalDate startDate, LocalDate endDate);
    
    String getSourceName();
    
    boolean isAvailable();
    
    String getSourceUrl();
}
