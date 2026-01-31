package com.asaf.whatnext.scraper;

import com.asaf.whatnext.services.EventSaveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class EventScraperService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EventScraperService.class);

    private final List<EventSource> scrapers;
    private final EventSaveService eventSaveService;

    @Autowired
    public EventScraperService(
            List<EventSource> scrapers,
            EventSaveService eventSaveService) {
        this.scrapers = scrapers;
        this.eventSaveService = eventSaveService;
        LOGGER.info("EventScraperService initialized with {} scrapers", scrapers.size());
    }

    public void scrapeAllEvents(String city) {
        LOGGER.info("Starting scrape for all sources{}", city != null ? " for city: " + city : "");
        
        for (EventSource scraper : scrapers) {
            scrapeAndSave(scraper, city);
        }
        
        LOGGER.info("Completed scraping all sources");
    }

    public void scrapeAllEvents(String city, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        
        LOGGER.info("Starting scrape for all sources, city: {}, dates: {} - {}", city, startDate, endDate);
        
        for (EventSource scraper : scrapers) {
            scrapeAndSave(scraper, city, startDate, endDate);
        }
        
        LOGGER.info("Completed scraping all sources");
    }

    public void scrapeEventsBySource(String sourceName, String city) {
        EventSource scraper = findScraper(sourceName);
        scrapeAndSave(scraper, city);
    }

    public void scrapeEventsBySource(String sourceName, String city, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        
        EventSource scraper = findScraper(sourceName);
        scrapeAndSave(scraper, city, startDate, endDate);
    }

    private void scrapeAndSave(EventSource scraper, String city) {
        String name = scraper.getSourceName();
        LOGGER.info("Starting: {}", name);
        
        try {
            if (!scraper.isAvailable()) {
                LOGGER.warn("{} is not available", name);
                return;
            }

            List<ScrapedEventData> data = city != null 
                ? scraper.scrapeEvents(city) 
                : scraper.scrapeEvents();
            
            LOGGER.info("{} returned {} events", name, data.size());
            
            int saved = eventSaveService.saveAll(data);
            LOGGER.info("{}: saved {}/{} events", name, saved, data.size());
            
        } catch (Exception e) {
            LOGGER.error("Error in {}: {}", name, e.getMessage(), e);
        }
    }

    private void scrapeAndSave(EventSource scraper, String city, LocalDate startDate, LocalDate endDate) {
        String name = scraper.getSourceName();
        LOGGER.info("Starting: {} for dates {} - {}", name, startDate, endDate);
        
        try {
            if (!scraper.isAvailable()) {
                LOGGER.warn("{} is not available", name);
                throw new RuntimeException(name + " is not available");
            }

            List<ScrapedEventData> data = scraper.scrapeEvents(city, startDate, endDate);
            
            LOGGER.info("{} returned {} events", name, data.size());
            
            int saved = eventSaveService.saveAll(data);
            LOGGER.info("{}: saved {}/{} events", name, saved, data.size());
            
        } catch (Exception e) {
            LOGGER.error("Error in {}: {}", name, e.getMessage(), e);
            throw new RuntimeException("Failed to scrape from " + name + ": " + e.getMessage(), e);
        }
    }

    private EventSource findScraper(String sourceName) {
        return scrapers.stream()
                .filter(s -> s.getSourceName().equalsIgnoreCase(sourceName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Scraper not found: " + sourceName));
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        
        if (startDate.isBefore(today)) {
            throw new IllegalArgumentException("Start date cannot be before today");
        }
        
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
    }

    public List<String> getAvailableScrapers() {
        return scrapers.stream()
                .map(EventSource::getSourceName)
                .toList();
    }
}
