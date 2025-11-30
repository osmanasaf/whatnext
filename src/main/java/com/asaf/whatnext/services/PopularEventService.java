package com.asaf.whatnext.services;

import com.asaf.whatnext.dto.EventSummaryDTO;
import com.asaf.whatnext.dto.WeeklyPopularEventsDTO;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.models.ConcertEvent;
import com.asaf.whatnext.models.PerformingArt;
import com.asaf.whatnext.repository.EventRepository;
import com.asaf.whatnext.services.scoring.EventScoringStrategy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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
        LocalDate nextWeek = today.plusDays(7);
        
        List<Event> weeklyEvents = eventRepository.findByStartDateBetweenAndCity(today, nextWeek, "istanbul");
        
        List<Event> concerts = filterAndSortEvents(weeklyEvents, EventType.CONCERT, 5);
        List<Event> theaters = filterAndSortEvents(weeklyEvents, EventType.THEATER, 5);
        List<Event> standups = filterAndSortEvents(weeklyEvents, EventType.STANDUP, 5);
        
        WeeklyPopularEventsDTO response = new WeeklyPopularEventsDTO();
        response.setConcerts(mapToDTOs(concerts));
        response.setTheaters(mapToDTOs(theaters));
        response.setStandups(mapToDTOs(standups));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM");
        response.setWeekRange(today.format(formatter) + " - " + nextWeek.format(formatter));
        
        return response;
    }
    
    private List<Event> filterAndSortEvents(List<Event> allEvents, EventType type, int limit) {
        return allEvents.stream()
                .filter(e -> e.getType() == type)
                .sorted(Comparator.comparingInt(scoringStrategy::calculateScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    private List<EventSummaryDTO> mapToDTOs(List<Event> events) {
        return events.stream().map(this::convertToDTO).collect(Collectors.toList());
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
        
        if (event instanceof PerformingArt) {
            dto.setPerformanceType(((PerformingArt) event).getPerformanceType());
        }
        
        return dto;
    }
}

