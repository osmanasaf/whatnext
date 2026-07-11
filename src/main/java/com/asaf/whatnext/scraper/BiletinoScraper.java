package com.asaf.whatnext.scraper;

import com.asaf.whatnext.config.BiletinoScraperConfig;
import com.asaf.whatnext.enums.Biletino.BiletinoCategory;
import com.asaf.whatnext.enums.Biletino.BiletinoCity;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.scraper.biletino.BiletinoCardParser;
import com.asaf.whatnext.scraper.biletino.BiletinoVenueEnricher;
import com.asaf.whatnext.services.ScrapeHealthService;
import com.asaf.whatnext.utils.WebScraperUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private final ScrapeHealthService scrapeHealthService;
    private final double minParseRatio;
    private final boolean enrichVenueFromDetail;

    @Autowired
    public BiletinoScraper(
            BiletinoScraperConfig config,
            ScrapeHealthService scrapeHealthService,
            @Value("${whatnext.scrape-health.min-parse-ratio:0.8}") double minParseRatio,
            @Value("${biletino.enrich-venue-from-detail:true}") boolean enrichVenueFromDetail) {
        super(config.getChromePath(), config.getWaitTimeoutSeconds(), BiletinoScraper.class);
        this.config = config;
        this.scrapeHealthService = scrapeHealthService;
        this.minParseRatio = minParseRatio;
        this.enrichVenueFromDetail = enrichVenueFromDetail;
    }

    @Override
    public List<ScrapedEventData> scrapeEvents(String city) {
        logger.info("Scraping events from Biletino for city: " + city);

        BiletinoCity selectedCity = parseCity(city);
        ScrapeRunStats stats = new ScrapeRunStats(getSourceName());

        List<CategoryScraper> scrapers = Arrays.asList(
            (driver, wait) -> scrapeCategory(BiletinoCategory.MUSIC, selectedCity, stats),
            (driver, wait) -> scrapeCategory(BiletinoCategory.THEATRE, selectedCity, stats),
            (driver, wait) -> scrapeCategory(BiletinoCategory.COMEDY, selectedCity, stats)
        );

        List<ScrapedEventData> allData = scrapeForCategories(scrapers);
        logger.info("Scraped " + allData.size() + " events from Biletino");

        long validCount = allData.stream().filter(ScrapedEventData::isValid).count();
        logger.info("Valid events: " + validCount + "/" + allData.size());

        finishAndRecordStats(stats, scrapeHealthService, minParseRatio);

        return allData;
    }

    private List<ScrapedEventData> scrapeCategory(BiletinoCategory category, BiletinoCity city, ScrapeRunStats stats) {
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
                stats.recordCardSeen();
                try {
                    ScrapedEventData data = cardParser.parseCard(card, category, city);
                    if (data != null && data.isValid()) {
                        enrichVenueIfNeeded(data);
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

    /**
     * Liste kartındaki mekân alanı ilçe adıdır (canlı denetimde doğrulandı, ör. "Kadıköy"); gerçek
     * mekân adı detay sayfası title etiketinden düz HTTP GET ile alınır. Başarısızlıkta mevcut değer korunur.
     */
    private void enrichVenueIfNeeded(ScrapedEventData data) {
        if (!enrichVenueFromDetail) {
            return;
        }
        try {
            String realVenue = BiletinoVenueEnricher.fetchVenueName(data.getTicketUrl());
            if (realVenue != null && !realVenue.isBlank()) {
                data.setVenueName(realVenue);
            }
        } catch (Exception e) {
            logger.debug("Biletino mekân zenginleştirme hatası: " + data.getTicketUrl(), e);
        }
    }

    private static String safeSnippet(WebElement card) {
        try {
            String html = card.getAttribute("outerHTML");
            return html == null ? "" : html.substring(0, Math.min(120, html.length()));
        } catch (Exception e) {
            return "";
        }
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
