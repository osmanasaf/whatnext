package com.asaf.whatnext.scraper;

import com.asaf.whatnext.config.BiletinialScraperConfig;
import com.asaf.whatnext.enums.Biletinial.BiletinialCategory;
import com.asaf.whatnext.enums.Biletinial.BiletinialCity;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.scraper.biletinial.BiletinialAddressVenueParser;
import com.asaf.whatnext.scraper.biletinial.BiletinialCardDraft;
import com.asaf.whatnext.scraper.biletinial.BiletinialDetailParser;
import com.asaf.whatnext.scraper.biletinial.BiletinialEventMapper;
import com.asaf.whatnext.utils.Texts;
import com.asaf.whatnext.utils.WebScraperUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Order(20)
public class BiletinialScraper extends BaseScraper {

    private static final String BASE_URL = "https://biletinial.com";
    private static final String SEARCH_URL_TEMPLATE = "%s/tr-tr/%s/%s?date=%s&enddate=%s";
    private static final String EVENT_CARD_SELECTOR = "#kategori__etkinlikler li";
    private static final String TITLE_SELECTOR = "h3 a";
    private static final String LOCATION_SELECTOR = "address";
    private static final String IMAGE_SELECTOR = "img[src*='merlincdn.net']";

    private final int scrapeDateRangeDays;

    @Autowired
    public BiletinialScraper(
            BiletinialScraperConfig config,
            @Value("${biletinial.scrape.date-range-days:14}") int scrapeDateRangeDays) {
        super(config.getChromePath(), config.getWaitTimeoutSeconds(), BiletinialScraper.class);
        this.scrapeDateRangeDays = scrapeDateRangeDays;
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

        for (int i = 0; i < scrapeDateRangeDays; i++) {
            LocalDate date = today.plusDays(i);
            String dateStr = date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

            String searchUrl = String.format(SEARCH_URL_TEMPLATE, BASE_URL, category.getValue(), cityValue, dateStr, dateStr);

            try {
                WebScraperUtils.navigateToUrl(driver, wait, searchUrl);
                WebScraperUtils.handleCookieConsent(driver);
                WebScraperUtils.handleOverlays(driver);

                List<WebElement> cards = driver.findElements(By.cssSelector(EVENT_CARD_SELECTOR));

                BiletinialDetailParser detailParser = new BiletinialDetailParser();
                for (WebElement card : cards) {
                    try {
                        BiletinialCardDraft draft = extractCardDraft(card, category, cityValue, date);
                        if (draft == null) {
                            continue;
                        }
                        BiletinialDetailParser.ParseResult detailResult =
                                detailParser.loadSessionsAndVenue(driver, wait, draft);
                        if (!detailResult.events().isEmpty()) {
                            events.addAll(detailResult.events());
                        } else {
                            BiletinialCardDraft merged = mergeWithVenueHint(
                                    draft, detailResult.venueTextFromPage());
                            ScrapedEventData fallback = BiletinialEventMapper.fromListOnly(merged);
                            if (fallback != null && fallback.isValid()) {
                                events.add(fallback);
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("Error extracting card", e);
                    }
                }

                checkAndRestartBrowserIfNeeded();

            } catch (Exception e) {
                logger.warn("Error scraping date: " + date, e);
            }
        }

        return events;
    }

    private BiletinialCardDraft extractCardDraft(
            WebElement card, BiletinialCategory category, String cityValue, LocalDate listDate) {
        try {
            String title = card.findElement(By.cssSelector(TITLE_SELECTOR)).getText();
            WebElement addressElement = card.findElement(By.cssSelector(LOCATION_SELECTOR));

            String city = addressElement.findElement(By.cssSelector("b")).getText();
            if (city != null && city.toLowerCase().contains("stanbul")) {
                city = "istanbul";
            }
            String venueName = BiletinialAddressVenueParser.extractVenueNameFromAddressHtml(
                    addressElement.getAttribute("outerHTML"));
            String href = card.findElement(By.cssSelector(TITLE_SELECTOR)).getAttribute("href");
            if (href == null || href.isEmpty()) {
                return null;
            }
            String ticketUrl = href.startsWith("http") ? href : BASE_URL + href;

            String imageUrl = null;
            try {
                imageUrl = card.findElement(By.cssSelector(IMAGE_SELECTOR)).getAttribute("src");
            } catch (Exception ignored) {
            }

            if (title.isEmpty()) {
                return null;
            }

            return new BiletinialCardDraft(
                    title.trim(), ticketUrl, imageUrl, venueName, city, category, cityValue, listDate);
        } catch (Exception e) {
            return null;
        }
    }

    private BiletinialCardDraft mergeWithVenueHint(BiletinialCardDraft d, String pageVenue) {
        String m = Texts.firstNonBlank(d.venueName(), pageVenue);
        return new BiletinialCardDraft(
                d.title(), d.ticketUrl(), d.imageUrl(), m, d.cityLabel(), d.category(), d.cityValue(), d.listDate());
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
