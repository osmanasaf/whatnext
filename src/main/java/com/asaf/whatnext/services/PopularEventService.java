package com.asaf.whatnext.services;

import com.asaf.whatnext.dto.EventSummaryDTO;
import com.asaf.whatnext.dto.WeeklyPopularEventsDTO;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.enums.PerformanceType;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.models.PerformingArt;
import com.asaf.whatnext.repository.EventRepository;
import com.asaf.whatnext.services.scoring.EventScoringStrategy;
import com.asaf.whatnext.utils.EventAudience;
import com.asaf.whatnext.utils.EventDuplicateKeys;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class PopularEventService {

    private final EventRepository eventRepository;
    private final EventScoringStrategy scoringStrategy;

    public PopularEventService(EventRepository eventRepository, EventScoringStrategy scoringStrategy) {
        this.eventRepository = eventRepository;
        this.scoringStrategy = scoringStrategy;
    }

    @Transactional(readOnly = true)
    public WeeklyPopularEventsDTO getWeeklyPopularEvents() {
        LocalDate today = LocalDate.now();
        int daysFromMonday = today.getDayOfWeek().getValue() - 1;
        LocalDate weekStart = today.minusDays(daysFromMonday);
        LocalDate weekEnd = weekStart.plusDays(6);
        
        if (today.getDayOfWeek().getValue() >= 4) {
            weekStart = weekStart.plusDays(7);
            weekEnd = weekEnd.plusDays(7);
        }
        
        List<Event> weeklyEvents = eventRepository.findByStartDateBetweenAndIstanbul(weekStart, weekEnd);
        
        if (weeklyEvents.size() < 5) {
            LocalDate extendedEnd = weekEnd.plusWeeks(2);
            weeklyEvents = eventRepository.findByStartDateBetweenAndIstanbul(weekStart, extendedEnd);
        }
        
        List<Event> concerts = filterAndSortEvents(weeklyEvents, EventType.CONCERT, 5);
        List<Event> theaters = filterAndSortEvents(weeklyEvents, EventType.THEATER, 5);
        List<Event> standups = filterAndSortEvents(weeklyEvents, EventType.STANDUP, 5);
        
        WeeklyPopularEventsDTO response = new WeeklyPopularEventsDTO();
        response.setConcerts(mapToDTOs(concerts));
        response.setTheaters(mapToDTOs(theaters));
        response.setStandups(mapToDTOs(standups));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM", Locale.forLanguageTag("tr"));
        response.setWeekRange(weekStart.format(formatter) + " - " + weekEnd.format(formatter));
        
        return response;
    }
    
    private List<Event> filterAndSortEvents(List<Event> allEvents, EventType type, int limit) {
        List<Event> sorted = allEvents.stream()
                .filter(e -> matchesEventType(e, type))
                .filter(e -> !EventAudience.isChildrensEvent(e))
                .sorted(Comparator.comparingInt(scoringStrategy::calculateScore).reversed())
                .toList();
        List<Event> deduped = EventDuplicateKeys.dedupeByTitleAndDateKeepOrder(sorted);
        return deduped.stream().limit(limit).toList();
    }

    private boolean matchesEventType(Event event, EventType type) {
        if (type == EventType.CONCERT) {
            return event.getType() == EventType.CONCERT;
        }
        if (event instanceof PerformingArt pa && pa.getPerformanceType() != null) {
            return (type == EventType.THEATER && pa.getPerformanceType() == PerformanceType.THEATER)
                || (type == EventType.STANDUP && pa.getPerformanceType() == PerformanceType.STANDUP);
        }
        return false;
    }
    
    private List<EventSummaryDTO> mapToDTOs(List<Event> events) {
        return events.stream().map(this::convertToDTO).toList();
    }
    
    private EventSummaryDTO convertToDTO(Event event) {
        EventSummaryDTO dto = new EventSummaryDTO();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setStartDate(event.getStartDate());
        dto.setStartTime(event.getStartTime());
        dto.setType(event.getType());
        dto.setCity(event.getCity());
        
        if (event.getVenue() != null) {
            dto.setVenueName(event.getVenue().getName());
        }
        
        if (event.getEventImage() != null) {
            dto.setImageUrl(event.getEventImage().getImageSourceUrl());
        }
        
        if (event instanceof PerformingArt performingArt) {
            dto.setPerformanceType(performingArt.getPerformanceType());
        }
        
        return dto;
    }
}

