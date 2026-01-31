package com.asaf.whatnext.scraper;

import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.utils.WebScraperUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class BaseScraper implements EventSource {
    
    protected final Logger logger;
    protected WebDriver driver;
    protected WebDriverWait wait;
    
    private final String chromePath;
    private final int waitTimeoutSeconds;
    
    protected BaseScraper(String chromePath, int waitTimeoutSeconds, Class<?> loggerClass) {
        this.chromePath = chromePath;
        this.waitTimeoutSeconds = waitTimeoutSeconds;
        this.logger = Logger.getLogger(loggerClass.getName());
    }
    
    protected void initializeBrowser() {
        if (driver == null) {
            logger.info("Initializing browser for " + getSourceName());
            this.driver = WebScraperUtils.initializeBrowser(chromePath, waitTimeoutSeconds);
            this.wait = new WebDriverWait(driver, Duration.ofSeconds(waitTimeoutSeconds));
        }
    }
    
    protected void closeBrowser() {
        if (driver != null) {
            WebScraperUtils.closeBrowser(driver);
            driver = null;
            wait = null;
        }
    }
    
    protected void checkAndRestartBrowserIfNeeded() {
        if (WebScraperUtils.shouldRestartBrowser()) {
            logger.info("Restarting browser to prevent memory leak");
            closeBrowser();
            initializeBrowser();
            WebScraperUtils.resetPageCounter();
        }
    }
    
    protected <T> T executeWithBrowser(BrowserOperation<T> operation) {
        try {
            initializeBrowser();
            return operation.execute(driver, wait);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during browser operation", e);
            throw new RuntimeException("Browser operation failed", e);
        } finally {
            closeBrowser();
        }
    }
    
    protected List<ScrapedEventData> scrapeForCategories(List<CategoryScraper> categoryScraper) {
        List<ScrapedEventData> allData = new ArrayList<>();
        
        try {
            initializeBrowser();
            
            for (CategoryScraper scraper : categoryScraper) {
                try {
                    List<ScrapedEventData> categoryData = scraper.scrape(driver, wait);
                    allData.addAll(categoryData);
                    checkAndRestartBrowserIfNeeded();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error scraping category", e);
                }
            }
            
        } finally {
            closeBrowser();
        }
        
        return allData;
    }
    
    @Override
    public List<ScrapedEventData> scrapeEvents() {
        return scrapeEvents(getDefaultCity());
    }
    
    @Override
    public abstract List<ScrapedEventData> scrapeEvents(String city);
    
    @Override
    public List<ScrapedEventData> scrapeEvents(String city, LocalDate startDate, LocalDate endDate) {
        List<ScrapedEventData> allData = scrapeEvents(city);
        return filterByDate(allData, startDate, endDate);
    }
    
    protected List<ScrapedEventData> filterByDate(List<ScrapedEventData> data, LocalDate startDate, LocalDate endDate) {
        List<ScrapedEventData> filtered = new ArrayList<>();
        for (ScrapedEventData item : data) {
            if (item.getStartDate() != null) {
                if ((item.getStartDate().isEqual(startDate) || item.getStartDate().isAfter(startDate)) &&
                    (item.getStartDate().isEqual(endDate) || item.getStartDate().isBefore(endDate))) {
                    filtered.add(item);
                }
            }
        }
        return filtered;
    }
    
    @Override
    public boolean isAvailable() {
        return true;
    }
    
    public abstract EventSourceType getEventSourceType();
    
    protected abstract String getDefaultCity();
    
    @FunctionalInterface
    protected interface BrowserOperation<T> {
        T execute(WebDriver driver, WebDriverWait wait) throws Exception;
    }
    
    @FunctionalInterface
    protected interface CategoryScraper {
        List<ScrapedEventData> scrape(WebDriver driver, WebDriverWait wait) throws Exception;
    }
}
