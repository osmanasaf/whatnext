package com.asaf.whatnext.scraper;

import com.asaf.whatnext.config.BiletinoScraperConfig;
import com.asaf.whatnext.enums.Biletino.BiletinoCategory;
import com.asaf.whatnext.enums.Biletino.BiletinoCity;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.utils.WebScraperUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import static com.asaf.whatnext.config.BiletinoScraperConfig.*;

@Service
public class BiletinoScraper extends BaseScraper {

    private final BiletinoScraperConfig config;

    @Autowired
    public BiletinoScraper(BiletinoScraperConfig config) {
        super(config.getChromePath(), config.getWaitTimeoutSeconds(), BiletinoScraper.class);
        this.config = config;
    }

    @Override
    public List<ScrapedEventData> scrapeEvents(String city) {
        logger.info("Scraping events from Biletino for city: " + city);
        
        BiletinoCity selectedCity = parseCity(city);
        
        List<CategoryScraper> scrapers = Arrays.asList(
            (driver, wait) -> scrapeCategory(BiletinoCategory.MUSIC, selectedCity),
            (driver, wait) -> scrapeCategory(BiletinoCategory.THEATRE, selectedCity),
            (driver, wait) -> scrapeCategory(BiletinoCategory.COMEDY, selectedCity)
        );
        
        List<ScrapedEventData> allData = scrapeForCategories(scrapers);
        logger.info("Scraped " + allData.size() + " events from Biletino");
        
        long validCount = allData.stream().filter(ScrapedEventData::isValid).count();
        logger.info("Valid events: " + validCount + "/" + allData.size());
        
        return allData;
    }

    private List<ScrapedEventData> scrapeCategory(BiletinoCategory category, BiletinoCity city) {
        List<ScrapedEventData> events = new ArrayList<>();
        
        try {
            String searchUrl = config.buildSearchUrl(category, city);
            WebScraperUtils.navigateToUrl(driver, wait, searchUrl);
            WebScraperUtils.handleCookieConsent(driver);
            WebScraperUtils.handleOverlays(driver);
            
            WebScraperUtils.scrollToLoadAll(
                driver,
                config.getMaxScrollAttempts(),
                config.getScrollWaitMs(),
                EVENT_CARD_SELECTOR
            );
            
            List<WebElement> cards = driver.findElements(By.cssSelector(EVENT_CARD_SELECTOR));
            
            for (WebElement card : cards) {
                try {
                    ScrapedEventData data = extractFromCard(card, category);
                    if (data != null && data.isValid()) {
                        events.add(data);
                    }
                } catch (Exception e) {
                    logger.log(Level.FINE, "Error extracting card", e);
                }
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error scraping category: " + category.name(), e);
        }
        
        return events;
    }

    private ScrapedEventData extractFromCard(WebElement card, BiletinoCategory category) {
        String cardHtml = card.getAttribute("outerHTML");
        Document doc = Jsoup.parse(cardHtml);
        
        String title = doc.select(EVENT_TITLE_SELECTOR).text();
        String dateStr = doc.select(EVENT_DATE_SELECTOR).text();
        String location = doc.select(EVENT_LOCATION_SELECTOR).text();
        String ticketUrl = BASE_URL + doc.select(EVENT_LINK_SELECTOR).attr("href");
        String imageUrl = doc.select(".event-image img").attr("src");
        
        if (title.isEmpty() || ticketUrl.equals(BASE_URL)) {
            return null;
        }
        
        String[] locationParts = parseLocation(location);
        String venueName = locationParts[0];
        String cityName = locationParts[1];
        
        EventType eventType = categoryToEventType(category);
        LocalDate parsedDate = parseDate(dateStr);
        
        if (parsedDate == null) {
            logger.fine("Failed to parse date: " + dateStr + " for event: " + title);
        }
        
        return ScrapedEventData.builder()
            .title(title)
            .rawDateString(dateStr)
            .startDate(parsedDate)
            .venueName(venueName)
            .location(location)
            .city(ScrapedEventData.normalizeCity(cityName))
            .ticketUrl(ticketUrl)
            .imageUrl(imageUrl)
            .eventType(eventType)
            .source(EventSourceType.BILETINO)
            .build();
    }

    private String[] parseLocation(String fullLocation) {
        String venueName = "";
        String city = "";
        
        if (fullLocation != null && !fullLocation.isEmpty()) {
            if (fullLocation.contains(",")) {
                String[] parts = fullLocation.split(",");
                venueName = parts[0].trim();
                if (parts.length > 1) {
                    city = parts[parts.length - 1].trim();
                }
            } else {
                city = fullLocation.trim();
            }
        }
        
        return new String[]{venueName, city};
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        
        try {
            String startPart = dateStr.contains("-") ? dateStr.split("-")[0].trim() : dateStr.trim();
            String[] tokens = startPart.split("\\s+");
            
            if (tokens.length >= 2) {
                String day = tokens[0];
                String month = tokens[1];
                int currentYear = LocalDate.now().getYear();
                
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);
                return LocalDate.parse(String.format("%s %s %d", day, month, currentYear), formatter);
            }
        } catch (DateTimeParseException e) {
            logger.log(Level.FINE, "Could not parse date: " + dateStr, e);
        }
        
        return null;
    }

    private EventType categoryToEventType(BiletinoCategory category) {
        return switch (category) {
            case MUSIC -> EventType.CONCERT;
            case THEATRE -> EventType.THEATER;
            case COMEDY -> EventType.STANDUP;
            default -> EventType.CONCERT;
        };
    }

    private BiletinoCity parseCity(String city) {
        return Arrays.stream(BiletinoCity.values())
                .filter(c -> c.getValue().equalsIgnoreCase(city))
                .findFirst()
                .orElse(BiletinoCity.ISTANBUL);
    }

    @Override
    public String getSourceName() {
        return "Biletino";
    }

    @Override
    public String getSourceUrl() {
        return BASE_URL;
    }

    @Override
    public EventSourceType getEventSourceType() {
        return EventSourceType.BILETINO;
    }

    @Override
    protected String getDefaultCity() {
        return BiletinoCity.ISTANBUL.getValue();
    }
}
