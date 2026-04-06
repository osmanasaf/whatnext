package com.asaf.whatnext.scraper;

import com.asaf.whatnext.config.BiletixScraperConfig;
import com.asaf.whatnext.enums.Biletix.BiletixCategory;
import com.asaf.whatnext.enums.Biletix.BiletixCity;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.enums.PerformanceType;
import com.asaf.whatnext.utils.WebScraperUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

@Service
public class BiletixScraper extends BaseScraper {

    private final BiletixScraperConfig config;

    @Autowired
    public BiletixScraper(BiletixScraperConfig config) {
        super(config.getChromePath(), config.getWaitTimeoutSeconds(), BiletixScraper.class);
        this.config = config;
    }

    @Override
    public List<ScrapedEventData> scrapeEvents(String city) {
        logger.info("Scraping events from Biletix for city: " + city);
        
        BiletixCity selectedCity = parseCity(city);
        
        List<CategoryScraper> scrapers = Arrays.asList(
            (driver, wait) -> scrapeCategory(BiletixCategory.MUSIC, selectedCity, null, null),
            (driver, wait) -> scrapeCategory(BiletixCategory.ART, selectedCity, null, null)
        );
        
        List<ScrapedEventData> allData = scrapeForCategories(scrapers);
        logger.info("Scraped " + allData.size() + " events from Biletix");
        
        long validCount = allData.stream().filter(ScrapedEventData::isValid).count();
        logger.info("Valid events: " + validCount + "/" + allData.size());
        
        return allData;
    }

    @Override
    public List<ScrapedEventData> scrapeEvents(String city, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return super.scrapeEvents(city, startDate, endDate);
        }
        logger.info("Scraping events from Biletix for city: " + city + " (" + startDate + " - " + endDate + ")");

        BiletixCity selectedCity = parseCity(city);

        List<CategoryScraper> scrapers = Arrays.asList(
            (driver, wait) -> scrapeCategory(BiletixCategory.MUSIC, selectedCity, startDate, endDate),
            (driver, wait) -> scrapeCategory(BiletixCategory.ART, selectedCity, startDate, endDate)
        );

        List<ScrapedEventData> allData = scrapeForCategories(scrapers);
        logger.info("Scraped " + allData.size() + " events from Biletix");

        long validCount = allData.stream().filter(ScrapedEventData::isValid).count();
        logger.info("Valid events: " + validCount + "/" + allData.size());

        return filterByDate(allData, startDate, endDate);
    }

    private List<ScrapedEventData> scrapeCategory(BiletixCategory category, BiletixCity city,
            LocalDate startDate, LocalDate endDate) {
        List<ScrapedEventData> events = new ArrayList<>();
        
        try {
            String searchUrl = config.buildSearchUrl(category, city, startDate, endDate);
            WebScraperUtils.navigateToUrl(driver, wait, searchUrl);
            
            WebScraperUtils.clickLoadMoreUntilEnd(
                driver, wait,
                BiletixScraperConfig.LOAD_MORE_BUTTON_SELECTOR,
                BiletixScraperConfig.EVENT_CARD_SELECTOR,
                config.getMaxLoadMoreAttempts(),
                config.getLoadMoreWaitMs()
            );
            
            List<WebElement> cards = driver.findElements(By.cssSelector(BiletixScraperConfig.EVENT_CARD_SELECTOR));
            
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

    private ScrapedEventData extractFromCard(WebElement card, BiletixCategory category) {
        String title = extractText(card, BiletixScraperConfig.EVENT_TITLE_SELECTOR);
        if (title.isEmpty()) {
            title = extractText(card, BiletixScraperConfig.EVENT_TITLE_MOBILE_SELECTOR);
        }
        
        if (title.isEmpty()) {
            return null;
        }
        
        String dateStr = extractDate(card);
        if (dateStr.isEmpty()) {
            return null;
        }
        
        String venueName = extractText(card, BiletixScraperConfig.EVENT_VENUE_SELECTOR);
        String location = extractText(card, BiletixScraperConfig.EVENT_LOCATION_SELECTOR);
        String ticketUrl = extractUrl(card);
        
        EventType eventType = determineEventType(title, category);
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
            .city(ScrapedEventData.normalizeCity(location))
            .ticketUrl(ticketUrl)
            .eventType(eventType)
            .source(EventSourceType.BILETIX)
            .build();
    }

    private String extractText(WebElement card, String selector) {
        try {
            return card.findElement(By.cssSelector(selector)).getText();
        } catch (Exception e) {
            return "";
        }
    }

    private String extractDate(WebElement card) {
        try {
            String secondDate = card.findElement(By.cssSelector(BiletixScraperConfig.EVENT_SECONDARY_DATE_SELECTOR)).getText();
            String firstDate = card.findElement(By.cssSelector(BiletixScraperConfig.EVENT_DATE_SELECTOR)).getText();
            return !secondDate.isEmpty() ? secondDate : firstDate;
        } catch (Exception e) {
            try {
                String day = card.findElement(By.cssSelector(".searchMobileDateDayNumber")).getText();
                String month = card.findElement(By.cssSelector(".searchMobileDateMonth")).getText();
                String year = card.findElement(By.cssSelector(".searchMobileDateYear")).getText();
                return day + "/" + month + "/" + year;
            } catch (Exception ex) {
                return "";
            }
        }
    }

    private String extractUrl(WebElement card) {
        try {
            return card.findElement(By.cssSelector(BiletixScraperConfig.EVENT_TITLE_SELECTOR)).getAttribute("href");
        } catch (Exception e) {
            try {
                return card.findElement(By.cssSelector(BiletixScraperConfig.EVENT_LINK_SELECTOR)).getAttribute("href");
            } catch (Exception ex) {
                return "";
            }
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        String normalized = dateStr.trim();
        int commaIndex = normalized.indexOf(',');
        if (commaIndex >= 0) {
            normalized = normalized.substring(commaIndex + 1).trim();
        }
        normalized = normalized.replaceAll("\\s+", "");

        List<String> patterns = Arrays.asList("dd/MM/yy", "dd/MM/yyyy");
        for (String pattern : patterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                return LocalDate.parse(normalized, formatter);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private EventType determineEventType(String title, BiletixCategory category) {
        if (category == BiletixCategory.MUSIC) {
            return EventType.CONCERT;
        }
        
        String lowerTitle = title.toLowerCase(Locale.forLanguageTag("tr"));
        if (lowerTitle.contains("stand") || lowerTitle.contains("komedi")) {
            return EventType.STANDUP;
        }
        
        return EventType.THEATER;
    }

    private BiletixCity parseCity(String city) {
        return Arrays.stream(BiletixCity.values())
                .filter(c -> c.getValue().equalsIgnoreCase(city))
                .findFirst()
                .orElse(BiletixCity.ISTANBUL);
    }

    @Override
    public String getSourceName() {
        return "Biletix";
    }

    @Override
    public String getSourceUrl() {
        return BiletixScraperConfig.BASE_URL;
    }

    @Override
    public EventSourceType getEventSourceType() {
        return EventSourceType.BILETIX;
    }

    @Override
    protected String getDefaultCity() {
        return BiletixCity.ISTANBUL.getValue();
    }
}
