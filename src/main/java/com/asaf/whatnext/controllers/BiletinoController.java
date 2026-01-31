package com.asaf.whatnext.controllers;

import com.asaf.whatnext.scraper.BiletinoScraper;
import com.asaf.whatnext.scraper.ScrapedEventData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/biletino")
public class BiletinoController extends BaseScraperController {

    @Autowired
    public BiletinoController(BiletinoScraper biletinoScraper) {
        super(biletinoScraper);
    }

    @GetMapping("/scrape/{city}")
    public ResponseEntity<List<ScrapedEventData>> scrapeByCity(@PathVariable String city) {
        List<ScrapedEventData> events = scraper.scrapeEvents(city);
        return ResponseEntity.ok(events);
    }
}
