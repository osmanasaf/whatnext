package com.asaf.whatnext.controllers;

import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.services.EventSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

public abstract class BaseScraperController {
    protected final EventSource scraper;

    protected BaseScraperController(EventSource scraper) {
        this.scraper = scraper;
    }

    @GetMapping("/events")
    public ResponseEntity<List<Event>> getAllEvents() {
        List<Event> events = scraper.fetchEvents();
        return ResponseEntity.ok(events);
    }

    @GetMapping("/check")
    public ResponseEntity<Boolean> checkAvailability() {
        boolean isAvailable = scraper.isAvailable();
        return ResponseEntity.ok(isAvailable);
    }
} 