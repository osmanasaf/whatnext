package com.asaf.whatnext.scraper;

import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.services.ScrapeHealthService;
import com.asaf.whatnext.utils.WebScraperUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseScraper implements EventSource {
    
    protected final Logger logger;
    protected WebDriver driver;
    protected WebDriverWait wait;
    
    private final String chromePath;
    private final int waitTimeoutSeconds;
    
    protected BaseScraper(String chromePath, int waitTimeoutSeconds, Class<?> loggerClass) {
        this.chromePath = chromePath;
        this.waitTimeoutSeconds = waitTimeoutSeconds;
        this.logger = LoggerFactory.getLogger(loggerClass);
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
            logger.error("Error during browser operation", e);
            throw new RuntimeException("Browser operation failed", e);
        } finally {
            closeBrowser();
        }
    }
    
    /**
     * Tek {@link WebDriver} örnekleri; paralel HTTP isteği aynı bean üzerinde yarışa girer. Tüm
     * kategori taramasını seri hale getirir.
     */
    protected List<ScrapedEventData> scrapeForCategories(List<CategoryScraper> categoryScraper) {
        synchronized (this) {
            return scrapeForCategoriesUnsynchronized(categoryScraper);
        }
    }

    private List<ScrapedEventData> scrapeForCategoriesUnsynchronized(List<CategoryScraper> categoryScraper) {
        List<ScrapedEventData> allData = new ArrayList<>();
        
        try {
            logger.info("Tarayıcı başlatılıyor (kaynak: " + getSourceName() + ", kategori sayısı: " + categoryScraper.size() + ")");
            initializeBrowser();
            logger.info("Tarayıcı hazır, tarama başlıyor: " + getSourceName());
            
            for (CategoryScraper scraper : categoryScraper) {
                try {
                    List<ScrapedEventData> categoryData = scraper.scrape(driver, wait);
                    allData.addAll(categoryData);
                    checkAndRestartBrowserIfNeeded();
                } catch (Exception e) {
                    logger.warn("Error scraping category", e);
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

    /**
     * Koşuyu bitirir, {@link ScrapeHealthService}'e kaydeder ve özet satırını loglar; eşik altında
     * WARN basar. Tüm kaynaklarda ({@code GET /api/scraper/health}) aynı davranış için ortak yol.
     */
    protected void finishAndRecordStats(ScrapeRunStats stats, ScrapeHealthService scrapeHealthService,
            double minParseRatio) {
        stats.finish();
        scrapeHealthService.recordRun(stats);
        logger.info(stats.toSummaryLine());
        if (!stats.isHealthy(minParseRatio)) {
            logger.warn(getSourceName() + " scrape sağlığı eşiğin altında (parseRatio<" + minParseRatio
                    + ") — site DOM'u değişmiş olabilir. " + stats.toSummaryLine());
        }
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
