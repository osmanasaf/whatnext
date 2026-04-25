package com.asaf.whatnext.scraper;

import com.asaf.whatnext.config.BiletinoScraperConfig;
import com.asaf.whatnext.enums.Biletino.BiletinoCategory;
import com.asaf.whatnext.enums.Biletino.BiletinoCity;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.scraper.biletino.BiletinoCardParser;
import com.asaf.whatnext.utils.WebScraperUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.asaf.whatnext.config.BiletinoScraperConfig.*;

@Service
@Order(30)
public class BiletinoScraper extends BaseScraper {

    private final BiletinoScraperConfig config;
    private final BiletinoCardParser cardParser = new BiletinoCardParser();

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
                    ScrapedEventData data = cardParser.parseCard(card, category, city);
                    if (data != null && data.isValid()) {
                        events.add(data);
                    }
                } catch (Exception e) {
                    logger.debug("Error extracting card", e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error scraping category: " + category.name(), e);
        }
        
        return events;
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
