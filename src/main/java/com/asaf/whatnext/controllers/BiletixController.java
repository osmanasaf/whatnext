package com.asaf.whatnext.controllers;

import com.asaf.whatnext.scraper.BiletixScraper;
import com.asaf.whatnext.scraper.ScrapedEventData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/biletix")
public class BiletixController extends BaseScraperController {

    @Autowired
    public BiletixController(BiletixScraper biletixScraper) {
        super(biletixScraper);
    }

    @GetMapping("/scrape/{city}")
    public ResponseEntity<List<ScrapedEventData>> scrapeByCity(@PathVariable String city) {
        List<ScrapedEventData> events = scraper.scrapeEvents(city);
        return ResponseEntity.ok(events);
    }
}
