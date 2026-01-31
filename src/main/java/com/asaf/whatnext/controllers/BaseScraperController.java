package com.asaf.whatnext.controllers;

import com.asaf.whatnext.scraper.EventSource;
import com.asaf.whatnext.scraper.ScrapedEventData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

public abstract class BaseScraperController {
    protected final EventSource scraper;

    protected BaseScraperController(EventSource scraper) {
        this.scraper = scraper;
    }

    @GetMapping("/scrape")
    public ResponseEntity<List<ScrapedEventData>> scrapeEvents() {
        List<ScrapedEventData> events = scraper.scrapeEvents();
        return ResponseEntity.ok(events);
    }

    @GetMapping("/check")
    public ResponseEntity<Boolean> checkAvailability() {
        boolean isAvailable = scraper.isAvailable();
        return ResponseEntity.ok(isAvailable);
    }
}
