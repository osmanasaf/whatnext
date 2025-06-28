package com.asaf.whatnext.controllers;

import com.asaf.whatnext.services.EventScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/scrapers")
public class ScraperController {

    private final EventScraperService eventScraperService;

    @Autowired
    public ScraperController(EventScraperService eventScraperService) {
        this.eventScraperService = eventScraperService;
    }

    @PostMapping("/run-all")
    public ResponseEntity<String> runAllScrapers(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            String usedCity = city != null ? city : "istanbul";
            LocalDate today = LocalDate.now();
            
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : today;
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : start.plusDays(6);
            
            if (start.isBefore(today)) {
                return ResponseEntity.badRequest().body("Başlangıç tarihi bugünden daha eski olamaz. Bugün: " + today);
            }
            if (end.isBefore(today)) {
                return ResponseEntity.badRequest().body("Bitiş tarihi bugünden daha eski olamaz. Bugün: " + today);
            }
            
            eventScraperService.scrapeAllEvents(usedCity, start, end);
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
    public ResponseEntity<String> runScraperBySource(
            @PathVariable String source,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            String usedCity = city != null ? city : "istanbul";
            LocalDate today = LocalDate.now();
            
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : today;
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : start.plusDays(6);
            
            // Bugünden eski tarih kontrolü
            if (start.isBefore(today)) {
                return ResponseEntity.badRequest().body("Başlangıç tarihi bugünden daha eski olamaz. Bugün: " + today);
            }
            if (end.isBefore(today)) {
                return ResponseEntity.badRequest().body("Bitiş tarihi bugünden daha eski olamaz. Bugün: " + today);
            }
            
            eventScraperService.scrapeEventsBySource(source, usedCity, start, end);
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