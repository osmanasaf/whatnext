package com.asaf.whatnext.scraper;

import com.asaf.whatnext.enums.AudienceType;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import lombok.Builder;
import lombok.Data;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Locale;

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
    private Integer minPriceTl;
    private String venueSlug;
    private AudienceType audience;

    public boolean isValid() {
        return title != null && !title.isEmpty() 
            && ticketUrl != null && !ticketUrl.isEmpty();
    }
    
    public static String normalizeCity(String city) {
        if (city == null || city.isEmpty()) {
            return null;
        }
        // Biletix NFD + bazı JVM'lerde İ→i dönüşümünde çıkan U+0307 — tek "istanbul" anahtarı için temizlenir
        String s = Normalizer.normalize(city.trim(), Normalizer.Form.NFC);
        s = s.replace("\u0307", "");
        s = s.toLowerCase(Locale.ROOT);
        s = s.replace("\u0307", "");
        s = s
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
        return s;
    }
}
