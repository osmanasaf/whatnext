package com.asaf.whatnext.scraper;

import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ScrapedEventData {
    
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String rawDateString;
    private String venueName;
    private String location;
    private String city;
    private String ticketUrl;
    private String imageUrl;
    private EventType eventType;
    private EventSourceType source;
    private String artistNames;
    
    public boolean isValid() {
        return title != null && !title.isEmpty() 
            && ticketUrl != null && !ticketUrl.isEmpty();
    }
    
    public static String normalizeCity(String city) {
        if (city == null || city.isEmpty()) {
            return null;
        }
        
        return city.trim().toLowerCase()
            .replace('ı', 'i')
            .replace('ğ', 'g')
            .replace('ü', 'u')
            .replace('ş', 's')
            .replace('ö', 'o')
            .replace('ç', 'c')
            .replace('İ', 'i')
            .replace('Ğ', 'g')
            .replace('Ü', 'u')
            .replace('Ş', 's')
            .replace('Ö', 'o')
            .replace('Ç', 'c');
    }
}
