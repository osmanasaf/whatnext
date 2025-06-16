package com.asaf.whatnext.services;

import com.asaf.whatnext.config.BiletixScraperConfig;
import com.asaf.whatnext.enums.Biletix.BiletixCategory;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.models.ConcertEvent;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.models.ExhibitionEvent;
import com.asaf.whatnext.models.PerformingArt;
import com.asaf.whatnext.service.VenueService;
import com.asaf.whatnext.utils.EventUtils;
import com.asaf.whatnext.utils.WebScraperUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class BiletixEventExtractor {
    private static final Logger LOGGER = Logger.getLogger(BiletixEventExtractor.class.getName());
    
    private final BiletixScraperConfig config;
    private final VenueService venueService;

    @Autowired
    public BiletixEventExtractor(BiletixScraperConfig config,
                                 VenueService venueService) {
        this.config = config;
        this.venueService = venueService;
    }
    
    public List<Event> extractBasicInfoFromCards(List<WebElement> eventCards, BiletixCategory category) {
        LOGGER.fine("Extracting basic info from event cards for category: " + category.name());
        List<Event> events = new ArrayList<>();

        for (WebElement card : eventCards) {
            Event event = extractBasicEventInfo(card, category);
            if (event != null) {
                events.add(event);
            }
        }

        LOGGER.info("Extracted basic info for " + events.size() + " events for category: " + category.name());
        return events;
    }
    
    private Event extractBasicEventInfo(WebElement card, BiletixCategory category) {
        try {
            EventInfo info = extractCardInfo(card);
            if (info == null) {
                LOGGER.warning("Failed to extract event info from card");
                return null;
            }

            if (info.getTitle().isEmpty()) {
                LOGGER.warning("Cannot create event without a title");
                return null;
            }

            EventType eventType = determineEventType(info.getTitle(), "", category);
            return createEventFromInfo(eventType, info);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error extracting basic event info", e);
            return null;
        }
    }
    
    private EventInfo extractCardInfo(WebElement card) {
        String title = extractTitle(card);
        String dateStr = extractDate(card);
        String venueName = extractVenueName(card);
        String location = extractLocation(card);
        String ticketUrl = findEventUrl(card);
        if (Objects.equals(dateStr, "")) {
            return null;
        }
        return new EventInfo(title, dateStr, venueName, location, ticketUrl);
    }
    
    private String extractTitle(WebElement card) {
        try {
            WebElement titleElement = card.findElement(By.cssSelector(BiletixScraperConfig.EVENT_TITLE_SELECTOR));
            return titleElement.getText();
        } catch (Exception e) {
            try {
                // Try mobile version
                return card.findElement(By.cssSelector(BiletixScraperConfig.EVENT_TITLE_MOBILE_SELECTOR)).getText();
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Error extracting event title", ex);
                return "";
            }
        }
    }
    
    private String extractDate(WebElement card) {
        try {
            String secondDate = card.findElement(By.cssSelector(BiletixScraperConfig.EVENT_SECONDARY_DATE_SELECTOR)).getText();
            String firstDate = card.findElement(By.cssSelector(BiletixScraperConfig.EVENT_DATE_SELECTOR)).getText();
            return Objects.equals(secondDate, "") ? firstDate : "";
        } catch (Exception e) {
            try {
                String day = card.findElement(By.cssSelector(".searchMobileDateDayNumber")).getText();
                String month = card.findElement(By.cssSelector(".searchMobileDateMonth")).getText();
                String year = card.findElement(By.cssSelector(".searchMobileDateYear")).getText();

                return day + "/" + EventUtils.getMonthNumber(month) + "/" + year;
            } catch (Exception ex) {
                LOGGER.log(Level.FINE, "Error extracting event date", ex);
                return "";
            }
        }
    }
    
    private String extractVenueName(WebElement card) {
        try {
            return card.findElement(By.cssSelector(BiletixScraperConfig.EVENT_VENUE_SELECTOR)).getText();
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error extracting venue name", e);
            return "";
        }
    }
    
    private String extractLocation(WebElement card) {
        try {
            return card.findElement(By.cssSelector(BiletixScraperConfig.EVENT_LOCATION_SELECTOR)).getText();
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error extracting location", e);
            return "";
        }
    }
    
    private String findEventUrl(WebElement card) {
        String eventUrl = null;

        try {
            WebElement nameElement = card.findElement(By.cssSelector(BiletixScraperConfig.EVENT_TITLE_SELECTOR));
            eventUrl = nameElement.getAttribute("href");
        } catch (Exception ex) {
            LOGGER.finest("Could not find URL in title element");
        }

        if (eventUrl == null || eventUrl.isEmpty()) {
            try {
                WebElement linkElement = card.findElement(By.cssSelector(BiletixScraperConfig.EVENT_LINK_SELECTOR));
                eventUrl = linkElement.getAttribute("href");
            } catch (Exception ex) {
                LOGGER.finest("Could not find URL in link element");
            }
        }

        return eventUrl;
    }
    
    private Event createEventFromInfo(EventType eventType, EventInfo info) {
        switch (eventType) {
            case CONCERT:
                return EventUtils.createConcertEvent(info, eventType, EventSourceType.BILETIX, venueService);
            case THEATER:
                return EventUtils.createTheaterEvent(info, eventType, EventSourceType.BILETIX);
            case STANDUP:
                return EventUtils.createTheaterEvent(info, eventType, EventSourceType.BILETIX);
            case EXHIBITION:
                return EventUtils.createExhibitionEvent(info, eventType, EventSourceType.BILETIX);
            default:
                LOGGER.warning("Unknown event type: " + eventType);
                return null;
        }
    }
    
    private EventType determineEventType(String title, String description, BiletixCategory category) {
        if (category == BiletixCategory.ART) {
            String lowerTitle = title.toLowerCase();
            String lowerDesc = description.toLowerCase();

            if (lowerTitle.contains("stand") || lowerDesc.contains("stand up") || 
                lowerTitle.contains("stand-up") || lowerDesc.contains("stand-up")) {
                return EventType.STANDUP;
            } else {
                return EventType.THEATER;
            }
        } else if (category == BiletixCategory.MUSIC) {
            return EventType.CONCERT;
        } else {
            return EventUtils.determineEventType(title, description, EventType.CONCERT);
        }
    }
    
    public DetailedEventInfo extractDetailedInfo(WebDriver driver) {
        String title = WebScraperUtils.extractTextWithMultipleSelectors(driver, "title", 
            "h1.eventTitle", 
            ".eventTitle", 
            "h1"
        );
        String description = WebScraperUtils.extractTextWithMultipleSelectors(driver, "description", 
            ".eventDescription", 
            ".description", 
            "div[class*='description']"
        );
        String dateStr = WebScraperUtils.extractTextWithMultipleSelectors(driver, "date", 
            ".eventDate", 
            ".date", 
            "div[class*='date']"
        );
        String venueName = WebScraperUtils.extractTextWithMultipleSelectors(driver, "venue name", 
            ".eventVenue", 
            ".venue", 
            "div[class*='venue']"
        );
        String location = WebScraperUtils.extractTextWithMultipleSelectors(driver, "location", 
            ".eventLocation", 
            ".location", 
            "div[class*='location']"
        );
        String ticketUrl = driver.getCurrentUrl();

        return new DetailedEventInfo(title, description, dateStr, venueName, location, ticketUrl);
    }
    
    public Event extractDetailedEventInfo(WebDriver driver) {
        try {
            DetailedEventInfo info = extractDetailedInfo(driver);

            if (info.getTitle().isEmpty()) {
                LOGGER.warning("Cannot create event without a title");
                return null;
            }

            EventType eventType = EventUtils.determineEventType(info.getTitle(), info.getDescription(), EventType.CONCERT);
            return createDetailedEventFromInfo(eventType, info);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error extracting detailed event info", e);
            return null;
        }
    }
    
    private Event createDetailedEventFromInfo(EventType eventType, DetailedEventInfo info) {
        switch (eventType) {
            case CONCERT:
                return EventUtils.createDetailedConcertEvent(info, eventType, EventSourceType.BILETIX, venueService);
            case THEATER:
                return EventUtils.createDetailedTheaterEvent(info, eventType, EventSourceType.BILETIX);
            case STANDUP:
                return EventUtils.createDetailedTheaterEvent(info, eventType, EventSourceType.BILETIX);
            case EXHIBITION:
                return EventUtils.createDetailedExhibitionEvent(info, eventType, EventSourceType.BILETIX);
            default:
                LOGGER.warning("Unknown event type: " + eventType);
                return null;
        }
    }
}