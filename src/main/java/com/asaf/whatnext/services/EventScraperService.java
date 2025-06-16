package com.asaf.whatnext.services;

import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.service.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EventScraperService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventScraperService.class);

    private final List<EventSource> scrapers;
    private final EventService eventService;

    @Autowired
    public EventScraperService(
            BiletixScraper biletixScraper,
            BiletinoScraper biletinoScraper,
            BiletinialScraper biletinialScraper,
            EventService eventService) {
        this.scrapers = new ArrayList<>();
        this.scrapers.add(biletinialScraper);
        this.scrapers.add(biletinoScraper);
        this.scrapers.add(biletixScraper);
        this.eventService = eventService;
    }

    public void scrapeAllEvents() {
        scrapeAllEvents(null);
    }

    public void scrapeAllEvents(String city) {
        LOGGER.info("Starting event scraping process{}...", city != null ? " for city: " + city : "");
        
        for (EventSource scraper : scrapers) {
            try {
                LOGGER.info("Starting scraper: {}", scraper.getSourceName());
                
                if (!scraper.isAvailable()) {
                    LOGGER.warn("Scraper {} is not available, skipping...", scraper.getSourceName());
                    continue;
                }

                List<Event> events = city != null ? scraper.fetchEvents(city) : scraper.fetchEvents();
                LOGGER.info("Found {} events from {}", events.size(), scraper.getSourceName());

                for (Event event : events) {
                    try {
                        eventService.save(event);
                        LOGGER.debug("Saved event: {} from {}", event.getTitle(), scraper.getSourceName());
                    } catch (Exception e) {
                        LOGGER.error("Error saving event {} from {}: {}", 
                            event.getTitle(), scraper.getSourceName(), e.getMessage());
                    }
                }

                LOGGER.info("Completed scraper: {}", scraper.getSourceName());
            } catch (Exception e) {
                LOGGER.error("Error in scraper {}: {}", scraper.getSourceName(), e.getMessage());
            }
        }
        
        LOGGER.info("Completed event scraping process");
    }

    public void scrapeEventsBySource(String sourceName) {
        scrapeEventsBySource(sourceName, null);
    }

    public void scrapeEventsBySource(String sourceName, String city) {
        LOGGER.info("Starting event scraping for source: {}{}", 
            sourceName, city != null ? " in city: " + city : "");
        
        EventSource targetScraper = scrapers.stream()
                .filter(scraper -> scraper.getSourceName().equalsIgnoreCase(sourceName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Scraper not found: " + sourceName));

        try {
            if (!targetScraper.isAvailable()) {
                LOGGER.warn("Scraper {} is not available", sourceName);
                return;
            }

            List<Event> events = city != null ? targetScraper.fetchEvents(city) : targetScraper.fetchEvents();
            LOGGER.info("Found {} events from {}", events.size(), sourceName);

            for (Event event : events) {
                try {
                    eventService.save(event);
                    LOGGER.debug("Saved event: {} from {}", event.getTitle(), sourceName);
                } catch (Exception e) {
                    LOGGER.error("Error saving event {} from {}: {}", 
                        event.getTitle(), sourceName, e.getMessage());
                }
            }

            LOGGER.info("Completed scraping for source: {}", sourceName);
        } catch (Exception e) {
            LOGGER.error("Error in scraper {}: {}", sourceName, e.getMessage());
        }
    }
} 