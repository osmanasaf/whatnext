package com.asaf.whatnext.services;

import com.asaf.whatnext.repository.EventRepository;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.models.Event;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class TitlePopularityService {

    private final EventRepository eventRepository;
    private static final int MONTHS_TO_ANALYZE = 2;
    private static final int MIN_EVENT_COUNT_FOR_POPULARITY = 3;
    private static final String ISTANBUL_PATTERN = "\\bistanbul\\b";
    
    private final Map<String, Integer> titlePopularityCache = new ConcurrentHashMap<>();
    private final Map<EventType, LocalDate> lastCacheUpdateByType = new ConcurrentHashMap<>();

    public TitlePopularityService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public int calculateTitlePopularityScore(String title, EventType eventType) {
        if (title == null || title.trim().isEmpty() || eventType == null) {
            return 0;
        }

        updateCacheIfNeeded(eventType);

        String normalizedTitle = normalizeTitle(title, eventType);
        
        String cacheKey = eventType.name() + ":" + normalizedTitle;
        return titlePopularityCache.getOrDefault(cacheKey, 0);
    }

    private void updateCacheIfNeeded(EventType eventType) {
        LocalDate today = LocalDate.now();
        LocalDate lastUpdate = lastCacheUpdateByType.get(eventType);
        
        if (lastUpdate == null || 
            java.time.temporal.ChronoUnit.DAYS.between(lastUpdate, today) >= 1) {
            refreshCache(eventType);
            lastCacheUpdateByType.put(eventType, today);
        }
    }

    private void refreshCache(EventType eventType) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusMonths(MONTHS_TO_ANALYZE);
        
        List<Event> recentEvents = eventRepository.findByStartDateBetweenAndType(
            startDate, today, eventType
        );

        Map<String, Long> titleCounts = recentEvents.stream()
            .filter(e -> e.getTitle() != null && !e.getTitle().trim().isEmpty())
            .collect(Collectors.groupingBy(
                e -> normalizeTitle(e.getTitle(), eventType),
                Collectors.counting()
            ));

        String prefix = eventType.name() + ":";
        titleCounts.forEach((normalizedTitle, count) -> {
            if (count >= MIN_EVENT_COUNT_FOR_POPULARITY) {
                int score;
                if (count >= 11) {
                    score = 15;
                } else if (count >= 6) {
                    score = 10;
                } else {
                    score = 5;
                }
                titlePopularityCache.put(prefix + normalizedTitle, score);
            }
        });
    }

    private String normalizeTitle(String title, EventType eventType) {
        if (title == null) return "";
        
        String normalized = title.toLowerCase().trim();
        
        switch (eventType) {
            case CONCERT:
                if (normalized.contains("-")) {
                    normalized = normalized.split("-")[0].trim();
                } else if (normalized.contains(":")) {
                    normalized = normalized.split(":")[0].trim();
                }
                normalized = normalized
                    .replaceAll("\\bkonser\\b", "")
                    .replaceAll("\\bconcert\\b", "")
                    .replaceAll(ISTANBUL_PATTERN, "")
                    .replaceAll("\\s+", " ")
                    .trim();
                break;
                
            case THEATER, STANDUP:
                if (normalized.contains("-")) {
                    normalized = normalized.split("-")[0].trim();
                } else if (normalized.contains(":")) {
                    normalized = normalized.split(":")[0].trim();
                }
                normalized = normalized
                    .replaceAll("\\btiyatro\\b", "")
                    .replaceAll("\\btheater\\b", "")
                    .replaceAll("\\bstand.?up\\b", "")
                    .replaceAll(ISTANBUL_PATTERN, "")
                    .replaceAll("\\s+", " ")
                    .trim();
                break;
                
            default:
                normalized = normalized
                    .replaceAll(ISTANBUL_PATTERN, "")
                    .replaceAll("\\s+", " ")
                    .trim();
        }
        
        return normalized;
    }

}

