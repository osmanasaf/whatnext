package com.asaf.whatnext.controllers;

import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.services.EventService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public List<Event> getAllEvents() {
        return eventService.getAllEvents();
    }

    @GetMapping("/biletix")
    public List<Event> getBiletixEvents() {
        return eventService.getBiletixEvents();
    }

    @GetMapping("/biletino")
    public List<Event> getBiletinoEvents() {
        return eventService.getBiletinoEvents();
    }

    // TODO: Diğer endpoint'ler eklenecek
    // - getEventById
    // - createEvent
    // - updateEvent
    // - deleteEvent
    // - getEventsByType
    // - getEventsByDateRange
    // - getEventsByLocation
} 
