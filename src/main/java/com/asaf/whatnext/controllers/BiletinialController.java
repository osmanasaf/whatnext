package com.asaf.whatnext.controllers;

import com.asaf.whatnext.enums.Biletinial.BiletinialCategory;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.services.BiletinialScraper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/biletinial")
public class BiletinialController extends BaseScraperController {

    private final BiletinialScraper biletinialScraper;

    @Autowired
    public BiletinialController(BiletinialScraper biletinialScraper) {
        super(biletinialScraper);
        this.biletinialScraper = biletinialScraper;
    }

    @GetMapping("/events/category/{category}")
    public ResponseEntity<List<Event>> getEventsByCategory(@PathVariable BiletinialCategory category) {
        List<Event> events = biletinialScraper.processCategory(category);
        return ResponseEntity.ok(events);
    }
} 