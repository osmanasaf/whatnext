package com.asaf.whatnext.controllers;

import com.asaf.whatnext.scraper.BubiletScraper;
import com.asaf.whatnext.scraper.ScrapedEventData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bubilet")
public class BubiletController extends BaseScraperController {

    @Autowired
    public BubiletController(BubiletScraper bubiletScraper) {
        super(bubiletScraper);
    }

    /**
     * Veritabanına yazmadan Bubilet scrape sonucunu döner (çok tarihli detay parse dahil).
     */
    @GetMapping("/scrape/{city}")
    public ResponseEntity<List<ScrapedEventData>> scrapeByCity(@PathVariable String city) {
        List<ScrapedEventData> events = scraper.scrapeEvents(city);
        return ResponseEntity.ok(events);
    }
}
