package com.asaf.whatnext.scraper;

import com.asaf.whatnext.config.BubiletScraperConfig;
import com.asaf.whatnext.enums.Bubilet.BubiletCategory;
import com.asaf.whatnext.enums.Bubilet.BubiletCity;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.utils.WebScraperUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;

@Service
public class BubiletScraper extends BaseScraper {

    private final BubiletScraperConfig config;

    @Autowired
    public BubiletScraper(BubiletScraperConfig config) {
        super(config.getChromePath(), config.getWaitTimeoutSeconds(), BubiletScraper.class);
        this.config = config;
    }

    @Override
    public List<ScrapedEventData> scrapeEvents(String city) {
        logger.info("Scraping events from Bubilet for city: " + city);
        
        BubiletCity selectedCity = parseCity(city);
        
        List<CategoryScraper> scrapers = Arrays.asList(
            (driver, wait) -> scrapeCategory(BubiletCategory.THEATRE, selectedCity),
            (driver, wait) -> scrapeCategory(BubiletCategory.MUSIC, selectedCity),
            (driver, wait) -> scrapeCategory(BubiletCategory.STANDUP, selectedCity)
        );
        
        List<ScrapedEventData> allData = scrapeForCategories(scrapers);
        logger.info("Scraped " + allData.size() + " events from Bubilet");
        
        long validCount = allData.stream().filter(ScrapedEventData::isValid).count();
        logger.info("Valid events: " + validCount + "/" + allData.size());
        
        return allData;
    }

    private List<ScrapedEventData> scrapeCategory(BubiletCategory category, BubiletCity city) {
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
                BubiletScraperConfig.EVENT_CARD_SELECTOR
            );
            
            List<WebElement> cards = driver.findElements(By.cssSelector(BubiletScraperConfig.EVENT_CARD_SELECTOR));
            
            for (WebElement card : cards) {
                try {
                    ScrapedEventData data = extractFromCard(card, category, city.getValue());
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

    private ScrapedEventData extractFromCard(WebElement card, BubiletCategory category, String city) {
        try {
            String title = card.findElement(By.cssSelector(BubiletScraperConfig.EVENT_TITLE_SELECTOR)).getText();
            String ticketUrl = card.getAttribute("href");
            
            if (title.isEmpty() || ticketUrl == null || ticketUrl.isEmpty()) {
                return null;
            }
            
            if (!ticketUrl.startsWith("http")) {
                ticketUrl = BubiletScraperConfig.BASE_URL + ticketUrl;
            }
            
            String dateText = "";
            try {
                dateText = card.findElement(By.cssSelector(BubiletScraperConfig.EVENT_DATE_SELECTOR)).getText();
            } catch (Exception e) {
                logger.log(Level.FINE, "Date not found for event: " + title);
            }
            
            if (!isSingleDateEvent(dateText)) {
                logger.fine("Skipping multi-date event: " + title + " (" + dateText + ")");
                return null;
            }
            
            String imageUrl = null;
            try {
                imageUrl = card.findElement(By.cssSelector(BubiletScraperConfig.EVENT_IMAGE_SELECTOR)).getAttribute("src");
            } catch (Exception ignored) {}
            
            LocalDate parsedDate = parseDate(dateText);
            
            if (parsedDate == null) {
                logger.fine("Failed to parse date: " + dateText + " for event: " + title);
            }
            
            EventType eventType = determineEventType(category);
            
            return ScrapedEventData.builder()
                .title(title)
                .rawDateString(dateText)
                .startDate(parsedDate)
                .ticketUrl(ticketUrl)
                .imageUrl(imageUrl)
                .eventType(eventType)
                .source(EventSourceType.BUBILET)
                .city(ScrapedEventData.normalizeCity(city))
                .build();
                
        } catch (Exception e) {
            logger.log(Level.FINE, "Error extracting from card", e);
            return null;
        }
    }

    private boolean isSingleDateEvent(String dateText) {
        if (dateText == null || dateText.isEmpty()) {
            return false;
        }
        return !dateText.contains(",") || dateText.contains(" - ");
    }

    private LocalDate parseDate(String dateText) {
        if (dateText == null || dateText.isEmpty()) {
            return null;
        }
        
        try {
            String[] parts = dateText.split(" - ");
            String datePart = parts[0].trim();
            
            String[] tokens = datePart.split("\\s+");
            
            if (tokens.length >= 2) {
                int day = Integer.parseInt(tokens[0]);
                String monthStr = tokens[1];
                int year = LocalDate.now().getYear();
                
                int month = getMonthNumber(monthStr);
                
                LocalDate date = LocalDate.of(year, month, day);
                
                if (date.isBefore(LocalDate.now().minusDays(30))) {
                    date = date.plusYears(1);
                }
                
                return date;
            }
        } catch (Exception e) {
            logger.log(Level.FINE, "Error parsing date: " + dateText, e);
        }
        
        return null;
    }

    private int getMonthNumber(String month) {
        return switch (month) {
            case "Ocak" -> 1;
            case "Şubat" -> 2;
            case "Mart" -> 3;
            case "Nisan" -> 4;
            case "Mayıs" -> 5;
            case "Haziran" -> 6;
            case "Temmuz" -> 7;
            case "Ağustos" -> 8;
            case "Eylül" -> 9;
            case "Ekim" -> 10;
            case "Kasım" -> 11;
            case "Aralık" -> 12;
            default -> 1;
        };
    }

    private EventType determineEventType(BubiletCategory category) {
        return switch (category) {
            case THEATRE -> EventType.THEATER;
            case MUSIC -> EventType.CONCERT;
            case STANDUP -> EventType.STANDUP;
        };
    }

    private BubiletCity parseCity(String city) {
        return Arrays.stream(BubiletCity.values())
                .filter(c -> c.getValue().equalsIgnoreCase(city))
                .findFirst()
                .orElse(BubiletCity.ISTANBUL);
    }

    @Override
    public String getSourceName() {
        return "Bubilet";
    }

    @Override
    public String getSourceUrl() {
        return BubiletScraperConfig.BASE_URL;
    }

    @Override
    public EventSourceType getEventSourceType() {
        return EventSourceType.BUBILET;
    }

    @Override
    protected String getDefaultCity() {
        return BubiletCity.ISTANBUL.getValue();
    }
}
