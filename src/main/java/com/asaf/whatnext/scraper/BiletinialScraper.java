package com.asaf.whatnext.scraper;

import com.asaf.whatnext.config.BiletinialScraperConfig;
import com.asaf.whatnext.enums.Biletinial.BiletinialCategory;
import com.asaf.whatnext.enums.Biletinial.BiletinialCity;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.utils.WebScraperUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;

@Service
public class BiletinialScraper extends BaseScraper {

    private static final String BASE_URL = "https://biletinial.com";
    private static final String SEARCH_URL_TEMPLATE = "%s/tr-tr/%s/%s?date=%s&enddate=%s";
    private static final String EVENT_CARD_SELECTOR = "#kategori__etkinlikler li";
    private static final String TITLE_SELECTOR = "h3 a";
    private static final String DATE_SELECTOR = "span";
    private static final String LOCATION_SELECTOR = "address";
    private static final String IMAGE_SELECTOR = "img[src*='merlincdn.net']";
    
    private final BiletinialScraperConfig config;

    @Autowired
    public BiletinialScraper(BiletinialScraperConfig config) {
        super(config.getChromePath(), config.getWaitTimeoutSeconds(), BiletinialScraper.class);
        this.config = config;
    }

    @Override
    public List<ScrapedEventData> scrapeEvents(String city) {
        logger.info("Scraping events from Biletinial for city: " + city);
        
        BiletinialCity selectedCity = parseCity(city);
        
        List<CategoryScraper> scrapers = new ArrayList<>();
        
        if (selectedCity == BiletinialCity.ISTANBUL) {
            scrapers.addAll(Arrays.asList(
                (driver, wait) -> scrapeCategory(BiletinialCategory.THEATRE, "istanbul-anadolu"),
                (driver, wait) -> scrapeCategory(BiletinialCategory.MUSIC, "istanbul-anadolu"),
                (driver, wait) -> scrapeCategory(BiletinialCategory.STANDUP, "istanbul-anadolu"),
                (driver, wait) -> scrapeCategory(BiletinialCategory.THEATRE, "istanbul-avrupa"),
                (driver, wait) -> scrapeCategory(BiletinialCategory.MUSIC, "istanbul-avrupa"),
                (driver, wait) -> scrapeCategory(BiletinialCategory.STANDUP, "istanbul-avrupa")
            ));
        } else {
            scrapers.addAll(Arrays.asList(
                (driver, wait) -> scrapeCategory(BiletinialCategory.THEATRE, selectedCity.getValue()),
                (driver, wait) -> scrapeCategory(BiletinialCategory.MUSIC, selectedCity.getValue()),
                (driver, wait) -> scrapeCategory(BiletinialCategory.STANDUP, selectedCity.getValue())
            ));
        }
        
        List<ScrapedEventData> allData = scrapeForCategories(scrapers);
        logger.info("Scraped " + allData.size() + " events from Biletinial");
        
        long validCount = allData.stream().filter(ScrapedEventData::isValid).count();
        logger.info("Valid events: " + validCount + "/" + allData.size());
        
        return allData;
    }

    private List<ScrapedEventData> scrapeCategory(BiletinialCategory category, String cityValue) {
        List<ScrapedEventData> events = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        for (int i = 0; i < 14; i++) {
            LocalDate date = today.plusDays(i);
            String dateStr = date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            
            String searchUrl = String.format(SEARCH_URL_TEMPLATE, BASE_URL, category.getValue(), cityValue, dateStr, dateStr);
            
            try {
                WebScraperUtils.navigateToUrl(driver, wait, searchUrl);
                
                List<WebElement> cards = driver.findElements(By.cssSelector(EVENT_CARD_SELECTOR));
                
                for (WebElement card : cards) {
                    try {
                        ScrapedEventData data = extractFromCard(card, category, date);
                        if (data != null && data.isValid()) {
                            events.add(data);
                        }
                    } catch (Exception e) {
                        logger.log(Level.FINE, "Error extracting card", e);
                    }
                }
                
                checkAndRestartBrowserIfNeeded();
                
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error scraping date: " + date, e);
            }
        }
        
        return events;
    }

    private ScrapedEventData extractFromCard(WebElement card, BiletinialCategory category, LocalDate date) {
        try {
            String title = card.findElement(By.cssSelector(TITLE_SELECTOR)).getText();
            WebElement addressElement = card.findElement(By.cssSelector(LOCATION_SELECTOR));

            String city = addressElement.findElement(By.cssSelector("b")).getText();
            if (city != null && city.toLowerCase().contains("stanbul")) {
                city = "istanbul";
            }
            String venueName = addressElement.findElement(By.cssSelector("small")).getText();
            String ticketUrl = BASE_URL + card.findElement(By.cssSelector(TITLE_SELECTOR)).getAttribute("href");
            
            String imageUrl = null;
            try {
                imageUrl = card.findElement(By.cssSelector(IMAGE_SELECTOR)).getAttribute("src");
            } catch (Exception ignored) {}
            
            if (title.isEmpty()) {
                return null;
            }

            EventType eventType = determineEventType(title, category);
            
            return ScrapedEventData.builder()
                .title(title)
                .rawDateString(date.toString())
                .startDate(date)
                .venueName(venueName)
                .location(venueName)
                .city(ScrapedEventData.normalizeCity(city))
                .ticketUrl(ticketUrl)
                .imageUrl(imageUrl)
                .eventType(eventType)
                .source(EventSourceType.BILETINIAL)
                .build();
                
        } catch (Exception e) {
            return null;
        }
    }

    private EventType determineEventType(String title, BiletinialCategory category) {
        String lowerTitle = title.toLowerCase();
        return switch (category) {
            case THEATRE -> lowerTitle.contains("stand") || lowerTitle.contains("stand-up") 
                ? EventType.STANDUP : EventType.THEATER;
            case MUSIC -> EventType.CONCERT;
            case STANDUP -> EventType.STANDUP;
            default -> EventType.THEATER;
        };
    }

    private BiletinialCity parseCity(String city) {
        return Arrays.stream(BiletinialCity.values())
                .filter(c -> c.getValue().equalsIgnoreCase(city))
                .findFirst()
                .orElse(BiletinialCity.ISTANBUL);
    }

    @Override
    public String getSourceName() {
        return "Biletinial";
    }

    @Override
    public String getSourceUrl() {
        return BASE_URL;
    }

    @Override
    public EventSourceType getEventSourceType() {
        return EventSourceType.BILETINIAL;
    }

    @Override
    protected String getDefaultCity() {
        return BiletinialCity.ISTANBUL.getValue();
    }
}
