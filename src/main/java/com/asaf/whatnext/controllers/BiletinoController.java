package com.asaf.whatnext.controllers;

import com.asaf.whatnext.enums.Biletino.BiletinoCategory;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.services.BiletinoScraper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/biletino")
public class BiletinoController extends BaseScraperController {

    private final BiletinoScraper biletinoScraper;

    @Autowired
    public BiletinoController(BiletinoScraper biletinoScraper) {
        super(biletinoScraper);
        this.biletinoScraper = biletinoScraper;
    }

    @GetMapping("/events/category/{category}")
    public ResponseEntity<List<Event>> getEventsByCategory(@PathVariable BiletinoCategory category) {
        List<Event> events = biletinoScraper.processCategory(category);
        return ResponseEntity.ok(events);
    }
} 