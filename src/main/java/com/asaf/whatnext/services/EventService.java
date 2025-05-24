package com.asaf.whatnext.services;

import com.asaf.whatnext.models.Event;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;

@Service
public class EventService {
    
    private final List<EventSource> eventSources;
    
    public EventService(List<EventSource> eventSources) {
        this.eventSources = eventSources;
    }
    
    public List<Event> getAllEvents() {
        List<Event> allEvents = new ArrayList<>();
        
        for (EventSource source : eventSources) {
            try {
                allEvents.addAll(source.fetchEvents());
            } catch (Exception e) {
                System.err.println("Error fetching events from " + source.getSourceName() + ": " + e.getMessage());
            }
        }
        
        return allEvents;
    }

    public List<Event> getBiletixEvents() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
        options.addArguments("--ignore-certificate-errors");

        WebDriver driver = new ChromeDriver(options);
        driver.get("https://www.google.com");
        System.out.println("URL açıldı: " + driver.getCurrentUrl());
        
        return eventSources.stream()
                .filter(BiletixScraper.class::isInstance)
                .findFirst()
                .map(EventSource::fetchEvents)
                .orElse(new ArrayList<>());
    }
} 