package com.asaf.whatnext.scraper;

import com.asaf.whatnext.config.BiletixScraperConfig;
import com.asaf.whatnext.enums.Biletix.BiletixCategory;
import com.asaf.whatnext.enums.Biletix.BiletixCity;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.scraper.biletix.BiletixCardParser;
import com.asaf.whatnext.services.ScrapeHealthService;
import com.asaf.whatnext.utils.WebScraperUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Order(40)
public class BiletixScraper extends BaseScraper {

    private final BiletixScraperConfig config;
    private final BiletixCardParser cardParser = new BiletixCardParser();
    private final ScrapeHealthService scrapeHealthService;
    private final double minParseRatio;

    @Autowired
    public BiletixScraper(
            BiletixScraperConfig config,
            ScrapeHealthService scrapeHealthService,
            @Value("${whatnext.scrape-health.min-parse-ratio:0.8}") double minParseRatio) {
        super(config.getChromePath(), config.getWaitTimeoutSeconds(), BiletixScraper.class);
        this.config = config;
        this.scrapeHealthService = scrapeHealthService;
        this.minParseRatio = minParseRatio;
    }

    @Override
    public List<ScrapedEventData> scrapeEvents(String city) {
        logger.info("Scraping events from Biletix for city: " + city);

        BiletixCity selectedCity = parseCity(city);
        ScrapeRunStats stats = new ScrapeRunStats(getSourceName());

        List<CategoryScraper> scrapers = Arrays.asList(
            (driver, wait) -> scrapeCategory(BiletixCategory.MUSIC, selectedCity, null, null, stats),
            (driver, wait) -> scrapeCategory(BiletixCategory.ART, selectedCity, null, null, stats)
        );

        List<ScrapedEventData> allData = scrapeForCategories(scrapers);
        logger.info("Scraped " + allData.size() + " events from Biletix");

        long validCount = allData.stream().filter(ScrapedEventData::isValid).count();
        logger.info("Valid events: " + validCount + "/" + allData.size());

        finishAndRecordStats(stats, scrapeHealthService, minParseRatio);

        return allData;
    }

    @Override
    public List<ScrapedEventData> scrapeEvents(String city, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return super.scrapeEvents(city, startDate, endDate);
        }
        logger.info("Scraping events from Biletix for city: " + city + " (" + startDate + " - " + endDate + ")");

        BiletixCity selectedCity = parseCity(city);
        ScrapeRunStats stats = new ScrapeRunStats(getSourceName());

        List<CategoryScraper> scrapers = Arrays.asList(
            (driver, wait) -> scrapeCategory(BiletixCategory.MUSIC, selectedCity, startDate, endDate, stats),
            (driver, wait) -> scrapeCategory(BiletixCategory.ART, selectedCity, startDate, endDate, stats)
        );

        List<ScrapedEventData> allData = scrapeForCategories(scrapers);
        logger.info("Scraped " + allData.size() + " events from Biletix");

        long validCount = allData.stream().filter(ScrapedEventData::isValid).count();
        logger.info("Valid events: " + validCount + "/" + allData.size());

        finishAndRecordStats(stats, scrapeHealthService, minParseRatio);

        return filterByDate(allData, startDate, endDate);
    }

    private List<ScrapedEventData> scrapeCategory(BiletixCategory category, BiletixCity city,
            LocalDate startDate, LocalDate endDate, ScrapeRunStats stats) {
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
                stats.recordCardSeen();
                try {
                    ScrapedEventData data = cardParser.parseCard(card, category);
                    if (data != null && data.isValid()) {
                        events.add(data);
                        stats.recordParsed(data.getTitle());
                        if (data.getVenueName() == null || data.getVenueName().isBlank()) {
                            stats.recordMissingVenue(data.getTitle());
                        }
                        if (data.getStartDate() == null) {
                            stats.recordMissingDate(data.getTitle());
                        }
                    } else {
                        stats.recordParseFailed(safeSnippet(card));
                    }
                } catch (Exception e) {
                    stats.recordParseFailed(safeSnippet(card));
                    logger.debug("Error extracting card", e);
                }
            }

        } catch (Exception e) {
            logger.error("Error scraping category: " + category.name(), e);
        }

        return events;
    }

    private static String safeSnippet(WebElement card) {
        try {
            String html = card.getAttribute("outerHTML");
            return html == null ? "" : html.substring(0, Math.min(120, html.length()));
        } catch (Exception e) {
            return "";
        }
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
