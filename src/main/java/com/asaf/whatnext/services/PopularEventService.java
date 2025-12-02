package com.asaf.whatnext.services;

import com.asaf.whatnext.dto.EventSummaryDTO;
import com.asaf.whatnext.dto.WeeklyPopularEventsDTO;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.models.PerformingArt;
import com.asaf.whatnext.repository.EventRepository;
import com.asaf.whatnext.services.scoring.EventScoringStrategy;
import org.springframework.stereotype.Service;

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

    public WeeklyPopularEventsDTO getWeeklyPopularEvents() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusMonths(2);
        
        List<Event> allRecentEvents = eventRepository.findByStartDateBetweenAndCity(
            startDate, today.plusDays(30), "istanbul"
        );
        
        int daysFromMonday = today.getDayOfWeek().getValue() - 1;
        LocalDate weekStart = today.minusDays(daysFromMonday);
        LocalDate weekEnd = weekStart.plusDays(6);
        
        if (today.getDayOfWeek().getValue() >= 4) {
            weekStart = weekStart.plusDays(7);
            weekEnd = weekEnd.plusDays(7);
        }
        
        final LocalDate finalWeekStart = weekStart;
        final LocalDate finalWeekEnd = weekEnd;
        List<Event> weeklyEvents = allRecentEvents.stream()
            .filter(e -> !e.getStartDate().isBefore(finalWeekStart) && !e.getStartDate().isAfter(finalWeekEnd))
            .toList();
        
        if (weeklyEvents.size() < 5) {
            LocalDate extendedEnd = weekEnd.plusWeeks(2);
            weeklyEvents = allRecentEvents.stream()
                .filter(e -> !e.getStartDate().isBefore(finalWeekStart) && !e.getStartDate().isAfter(extendedEnd))
                .toList();
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
        return allEvents.stream()
                .filter(e -> e.getType() == type)
                .sorted(Comparator.comparingInt(scoringStrategy::calculateScore).reversed())
                .limit(limit)
                .toList();
    }
    
    private List<EventSummaryDTO> mapToDTOs(List<Event> events) {
        return events.stream().map(this::convertToDTO).toList();
    }
    
    private EventSummaryDTO convertToDTO(Event event) {
        EventSummaryDTO dto = new EventSummaryDTO();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setStartDate(event.getStartDate());
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

