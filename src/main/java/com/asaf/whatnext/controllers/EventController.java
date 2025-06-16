package com.asaf.whatnext.controllers;

import com.asaf.whatnext.dto.EventSummaryDTO;
import com.asaf.whatnext.dto.EventDetailDTO;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.enums.City;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.models.EventImage;
import com.asaf.whatnext.models.Venue;
import com.asaf.whatnext.repository.EventImageRepository;
import com.asaf.whatnext.service.EventService;
import com.asaf.whatnext.service.VenueService;
import com.asaf.whatnext.mapper.EventMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final VenueService venueService;
    private final EventImageRepository eventImageRepository;
    private final EventMapper eventMapper;

    @Autowired
    public EventController(EventService eventService, VenueService venueService, EventImageRepository eventImageRepository, EventMapper eventMapper) {
        this.eventService = eventService;
        this.venueService = venueService;
        this.eventImageRepository = eventImageRepository;
        this.eventMapper = eventMapper;
    }

    @GetMapping
    public ResponseEntity<List<EventSummaryDTO>> getAllEvents() {
        List<Event> events = eventService.findAll();
        List<EventSummaryDTO> dtos = events.stream()
            .map(eventMapper::toSummaryDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDetailDTO> getEventById(@PathVariable Long id) {
        return eventService.findById(id)
            .map(eventMapper::toDetailDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<EventSummaryDTO>> getEventsByType(@PathVariable EventType type) {
        List<Event> events = eventService.findByType(type);
        List<EventSummaryDTO> dtos = events.stream()
            .map(eventMapper::toSummaryDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/source/{source}")
    public ResponseEntity<List<EventSummaryDTO>> getEventsBySource(@PathVariable EventSourceType source) {
        List<Event> events = eventService.findBySource(source);
        List<EventSummaryDTO> dtos = events.stream()
            .map(eventMapper::toSummaryDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/city/{city}")
    public ResponseEntity<List<EventSummaryDTO>> getEventsByCity(@PathVariable City city) {
        List<Event> events = eventService.findByCity(city);
        List<EventSummaryDTO> dtos = events.stream()
            .map(eventMapper::toSummaryDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<EventSummaryDTO>> getEventsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<Event> events = eventService.findByDate(date.toString());
        List<EventSummaryDTO> dtos = events.stream()
            .map(eventMapper::toSummaryDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/weekly")
    public ResponseEntity<List<EventSummaryDTO>> getWeeklyEvents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) EventType type,
            @RequestParam(required = false) City city) {
        
        LocalDate targetStartDate = startDate != null ? startDate : LocalDate.now();
        LocalDate targetEndDate = targetStartDate.plusDays(6);
        
        String startDateStr = targetStartDate.toString();
        String endDateStr = targetEndDate.toString();

        List<Event> events;
        if (type != null && city != null) {
            events = eventService.findByDateRangeAndTypeAndCity(startDateStr, endDateStr, type, city);
        } else if (type != null) {
            events = eventService.findByDateRangeAndType(startDateStr, endDateStr, type);
        } else if (city != null) {
            events = eventService.findByDateRangeAndCity(startDateStr, endDateStr, city);
        } else {
            events = eventService.findByDateRange(startDateStr, endDateStr);
        }

        List<EventSummaryDTO> dtos = events.stream()
            .map(eventMapper::toSummaryDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<EventSummaryDTO>> getMonthlyEvents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) EventType type,
            @RequestParam(required = false) City city) {
        
        LocalDate targetStartDate = startDate != null ? startDate : LocalDate.now();
        LocalDate targetEndDate = targetStartDate.plusMonths(1).minusDays(1);
        
        String startDateStr = targetStartDate.toString();
        String endDateStr = targetEndDate.toString();

        List<Event> events;
        if (type != null && city != null) {
            events = eventService.findByDateRangeAndTypeAndCity(startDateStr, endDateStr, type, city);
        } else if (type != null) {
            events = eventService.findByDateRangeAndType(startDateStr, endDateStr, type);
        } else if (city != null) {
            events = eventService.findByDateRangeAndCity(startDateStr, endDateStr, city);
        } else {
            events = eventService.findByDateRange(startDateStr, endDateStr);
        }

        List<EventSummaryDTO> dtos = events.stream()
            .map(eventMapper::toSummaryDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/range")
    public ResponseEntity<List<EventSummaryDTO>> getEventsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) EventType type,
            @RequestParam(required = false) City city) {
        
        String startDateStr = startDate.toString();
        String endDateStr = endDate != null ? endDate.toString() : startDateStr;

        List<Event> events;
        if (type != null && city != null) {
            events = eventService.findByDateRangeAndTypeAndCity(startDateStr, endDateStr, type, city);
        } else if (type != null) {
            events = eventService.findByDateRangeAndType(startDateStr, endDateStr, type);
        } else if (city != null) {
            events = eventService.findByDateRangeAndCity(startDateStr, endDateStr, city);
        } else {
            events = eventService.findByDateRange(startDateStr, endDateStr);
        }

        List<EventSummaryDTO> dtos = events.stream()
            .map(eventMapper::toSummaryDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<EventImage> getEventImage(@PathVariable Long id) {
        return eventImageRepository.findByEventId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/venue")
    public ResponseEntity<Venue> getEventVenue(@PathVariable Long id) {
        return eventService.findById(id)
                .map(event -> ResponseEntity.ok(event.getVenue()))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteById(id);
        return ResponseEntity.ok().build();
    }
} 
