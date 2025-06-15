package com.asaf.whatnext.controllers;

import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.models.EventImage;
import com.asaf.whatnext.models.Stage;
import com.asaf.whatnext.models.Venue;
import com.asaf.whatnext.repository.EventImageRepository;
import com.asaf.whatnext.service.EventService;
import com.asaf.whatnext.service.VenueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final VenueService venueService;
    private final EventImageRepository eventImageRepository;

    @Autowired
    public EventController(EventService eventService, VenueService venueService, EventImageRepository eventImageRepository) {
        this.eventService = eventService;
        this.venueService = venueService;
        this.eventImageRepository = eventImageRepository;
    }

    @GetMapping
    public ResponseEntity<List<Event>> getAllEvents() {
        return ResponseEntity.ok(eventService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable Long id) {
        return eventService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<Event>> getEventsByType(@PathVariable EventType type) {
        return ResponseEntity.ok(eventService.findByType(type));
    }

    @GetMapping("/source/{source}")
    public ResponseEntity<List<Event>> getEventsBySource(@PathVariable EventSourceType source) {
        return ResponseEntity.ok(eventService.findBySource(source));
    }

    @GetMapping("/venue/{venueId}")
    public ResponseEntity<List<Event>> getEventsByVenue(@PathVariable Long venueId) {
        return venueService.findById(venueId)
                .map(venue -> ResponseEntity.ok(eventService.findByVenue(venue)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<Event>> getEventsByDate(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(eventService.findByDate(date.toString()));
    }

    @GetMapping("/city/{city}")
    public ResponseEntity<List<Event>> getEventsByCity(@PathVariable String city) {
        return ResponseEntity.ok(eventService.findByCity(city));
    }

    @GetMapping("/daily")
    public ResponseEntity<List<Event>> getDailyEvents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String city) {
        
        LocalDate targetDate = date != null ? date : LocalDate.now();
        String dateStr = targetDate.toString();

        if (type != null && city != null) {
            return ResponseEntity.ok(eventService.findByDateRangeAndTypeAndCity(dateStr, dateStr, type, city));
        } else if (type != null) {
            return ResponseEntity.ok(eventService.findByDateRangeAndType(dateStr, dateStr, type));
        } else if (city != null) {
            return ResponseEntity.ok(eventService.findByDateRangeAndCity(dateStr, dateStr, city));
        } else {
            return ResponseEntity.ok(eventService.findByDate(dateStr));
        }
    }

    @GetMapping("/weekly")
    public ResponseEntity<List<Event>> getWeeklyEvents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String city) {
        
        LocalDate targetStartDate = startDate != null ? startDate : LocalDate.now();
        LocalDate targetEndDate = targetStartDate.plusDays(6);
        
        String startDateStr = targetStartDate.toString();
        String endDateStr = targetEndDate.toString();

        if (type != null && city != null) {
            return ResponseEntity.ok(eventService.findByDateRangeAndTypeAndCity(startDateStr, endDateStr, type, city));
        } else if (type != null) {
            return ResponseEntity.ok(eventService.findByDateRangeAndType(startDateStr, endDateStr, type));
        } else if (city != null) {
            return ResponseEntity.ok(eventService.findByDateRangeAndCity(startDateStr, endDateStr, city));
        } else {
            return ResponseEntity.ok(eventService.findByDateRange(startDateStr, endDateStr));
        }
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<Event>> getMonthlyEvents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String city) {
        
        LocalDate targetStartDate = startDate != null ? startDate : LocalDate.now();
        LocalDate targetEndDate = targetStartDate.plusMonths(1).minusDays(1);
        
        String startDateStr = targetStartDate.toString();
        String endDateStr = targetEndDate.toString();

        if (type != null && city != null) {
            return ResponseEntity.ok(eventService.findByDateRangeAndTypeAndCity(startDateStr, endDateStr, type, city));
        } else if (type != null) {
            return ResponseEntity.ok(eventService.findByDateRangeAndType(startDateStr, endDateStr, type));
        } else if (city != null) {
            return ResponseEntity.ok(eventService.findByDateRangeAndCity(startDateStr, endDateStr, city));
        } else {
            return ResponseEntity.ok(eventService.findByDateRange(startDateStr, endDateStr));
        }
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<EventImage> getEventImage(@PathVariable Long id) {
        return eventImageRepository.findByEventId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/stages")
    public ResponseEntity<List<Stage>> getEventStages(@PathVariable Long id) {
        return eventService.findById(id)
                .map(event -> ResponseEntity.ok(event.getStages()))
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
