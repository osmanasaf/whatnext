package com.asaf.whatnext.controllers;

import com.asaf.whatnext.services.EventScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scrapers")
public class ScraperController {

    private final EventScraperService eventScraperService;

    @Autowired
    public ScraperController(EventScraperService eventScraperService) {
        this.eventScraperService = eventScraperService;
    }

    @PostMapping("/run-all")
    public ResponseEntity<String> runAllScrapers() {
        try {
            eventScraperService.scrapeAllEvents();
            return ResponseEntity.ok("All scrapers completed successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error running scrapers: " + e.getMessage());
        }
    }

    @PostMapping("/run-all/{city}")
    public ResponseEntity<String> runAllScrapersForCity(@PathVariable String city) {
        try {
            eventScraperService.scrapeAllEvents(city);
            return ResponseEntity.ok("All scrapers completed successfully for city: " + city);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error running scrapers: " + e.getMessage());
        }
    }

    @PostMapping("/run/{source}")
    public ResponseEntity<String> runScraperBySource(@PathVariable String source) {
        try {
            eventScraperService.scrapeEventsBySource(source);
            return ResponseEntity.ok("Scraper " + source + " completed successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error running scraper: " + e.getMessage());
        }
    }

    @PostMapping("/run/{source}/{city}")
    public ResponseEntity<String> runScraperBySourceAndCity(@PathVariable String source, @PathVariable String city) {
        try {
            eventScraperService.scrapeEventsBySource(source, city);
            return ResponseEntity.ok("Scraper " + source + " completed successfully for city: " + city);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error running scraper: " + e.getMessage());
        }
    }
} 