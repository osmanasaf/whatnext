package com.asaf.whatnext.controllers;

import com.asaf.whatnext.enums.Biletix.BiletixCategory;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.services.BiletixScraper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/biletix")
public class BiletixController extends BaseScraperController {

    private final BiletixScraper biletixScraper;

    @Autowired
    public BiletixController(BiletixScraper biletixScraper) {
        super(biletixScraper);
        this.biletixScraper = biletixScraper;
    }

    @GetMapping("/events/category/{category}")
    public ResponseEntity<List<Event>> getEventsByCategory(@PathVariable BiletixCategory category) {
        List<Event> events = biletixScraper.processCategory(category);
        return ResponseEntity.ok(events);
    }
} 