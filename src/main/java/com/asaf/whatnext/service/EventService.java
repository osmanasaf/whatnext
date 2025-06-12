package com.asaf.whatnext.service;

import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {
    private final EventRepository eventRepository;

    @Autowired
    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional
    public Event save(Event newEvent) {
        // Duplicate kontrolü
        Optional<Event> existingEvent = findDuplicateEvent(newEvent);
        
        if (existingEvent.isPresent()) {
            Event event = existingEvent.get();
            // Yeni URL'yi ekle
            event.addTicketUrl(newEvent.getTicketUrls().getFirst());
            return eventRepository.save(event);
        }

        return eventRepository.save(newEvent);
    }

    private Optional<Event> findDuplicateEvent(Event newEvent) {
        // Aynı tarih ve mekandaki etkinlikleri getir
        List<Event> sameDateEvents = eventRepository.findByStartDateAndVenue(
            newEvent.getStartDate(), 
            newEvent.getVenue()
        );

        // Benzerlik kontrolü
        return sameDateEvents.stream()
            .filter(event -> isSimilarEvent(event, newEvent))
            .findFirst();
    }

    private boolean isSimilarEvent(Event event1, Event event2) {
        // Başlık benzerliği kontrolü
        String title1 = normalizeTitle(event1.getTitle());
        String title2 = normalizeTitle(event2.getTitle());
        
        // Levenshtein mesafesi ile benzerlik kontrolü
        double similarity = calculateSimilarity(title1, title2);
        
        // Benzerlik oranı %80'den fazlaysa aynı etkinlik olarak kabul et
        return similarity > 0.8;
    }

    private String normalizeTitle(String title) {
        return title.toLowerCase()
            .replaceAll("[^a-z0-9\\s]", "") // Özel karakterleri kaldır
            .replaceAll("\\s+", " ")        // Fazla boşlukları tek boşluğa indir
            .trim();
    }

    private double calculateSimilarity(String s1, String s2) {
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0;
        
        int distance = levenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLength);
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }

    public List<Event> findAll() {
        return eventRepository.findAll();
    }

    public Event findById(Long id) {
        return eventRepository.findById(id).orElse(null);
    }

    public List<Event> findByType(String type) {
        return eventRepository.findByType(EventType.valueOf(type));
    }

    public List<Event> findBySource(String source) {
        return eventRepository.findBySource(EventSourceType.valueOf(source));
    }

    public List<Event> findByVenueId(Long venueId) {
        return eventRepository.findByVenueId(venueId);
    }

    public List<Event> findByDate(String date) {
        LocalDate localDate = LocalDate.parse(date);
        return eventRepository.findByStartDate(localDate);
    }

    public void deleteById(Long id) {
        eventRepository.deleteById(id);
    }
} 