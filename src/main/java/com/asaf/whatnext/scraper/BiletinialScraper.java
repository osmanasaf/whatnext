package com.asaf.whatnext.scraper;

import com.asaf.whatnext.config.BiletinialScraperConfig;
import com.asaf.whatnext.enums.Biletinial.BiletinialCategory;
import com.asaf.whatnext.enums.Biletinial.BiletinialCity;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.utils.WebScraperUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;

@Service
public class BiletinialScraper extends BaseScraper {

    private static final String BASE_URL = "https://biletinial.com";
    private static final String SEARCH_URL_TEMPLATE = "%s/tr-tr/%s/%s?date=%s&enddate=%s";
    private static final String EVENT_CARD_SELECTOR = "#kategori__etkinlikler li";
    private static final String TITLE_SELECTOR = "h3 a";
    private static final String LOCATION_SELECTOR = "address";
    private static final String IMAGE_SELECTOR = "img[src*='merlincdn.net']";

    /** Detay — bilet / seans (schema.org Event) */
    private static final String DETAIL_TICKET_SECTION_ID = "info_ticket";
    private static final String DETAIL_CITY_BLOCK = ".ed-biletler__sehir[data-sehir]";
    private static final String DETAIL_SESSION_BLOCK = ".ed-biletler__sehir__gun";
    private static final String DETAIL_START_DATE = "time[itemprop='startDate']";
    private static final String DETAIL_VENUE_ADDRESS = "address[itemprop='name']";

    @Autowired
    public BiletinialScraper(BiletinialScraperConfig config) {
        super(config.getChromePath(), config.getWaitTimeoutSeconds(), BiletinialScraper.class);
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
                WebScraperUtils.handleCookieConsent(driver);
                WebScraperUtils.handleOverlays(driver);

                List<WebElement> cards = driver.findElements(By.cssSelector(EVENT_CARD_SELECTOR));

                for (WebElement card : cards) {
                    try {
                        CardDraft draft = extractCardDraft(card, category, cityValue, date);
                        if (draft == null) {
                            continue;
                        }
                        List<ScrapedEventData> fromDetail = fetchSessionsFromDetail(draft);
                        if (!fromDetail.isEmpty()) {
                            events.addAll(fromDetail);
                        } else {
                            ScrapedEventData fallback = buildFromDraftListOnly(draft);
                            if (fallback != null && fallback.isValid()) {
                                events.add(fallback);
                            }
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

    private CardDraft extractCardDraft(WebElement card, BiletinialCategory category, String cityValue, LocalDate listDate) {
        try {
            String title = card.findElement(By.cssSelector(TITLE_SELECTOR)).getText();
            WebElement addressElement = card.findElement(By.cssSelector(LOCATION_SELECTOR));

            String city = addressElement.findElement(By.cssSelector("b")).getText();
            if (city != null && city.toLowerCase().contains("stanbul")) {
                city = "istanbul";
            }
            String venueName = addressElement.findElement(By.cssSelector("small")).getText();
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

            return new CardDraft(title.trim(), ticketUrl, imageUrl, venueName, city, category, cityValue, listDate);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Detay sayfasındaki schema.org seanslarından, liste bölgesi + liste günü ile eşleşenleri üretir.
     */
    private List<ScrapedEventData> fetchSessionsFromDetail(CardDraft d) {
        List<ScrapedEventData> out = new ArrayList<>();
        try {
            WebScraperUtils.navigateToUrl(driver, wait, d.ticketUrl());
            WebScraperUtils.handleCookieConsent(driver);
            WebScraperUtils.handleOverlays(driver);

            wait.until(ExpectedConditions.presenceOfElementLocated(By.id(DETAIL_TICKET_SECTION_ID)));

            List<WebElement> cityBlocks = driver.findElements(By.cssSelector(DETAIL_CITY_BLOCK));
            Set<String> seenSessionKeys = new LinkedHashSet<>();

            for (WebElement block : cityBlocks) {
                String dataSehir = block.getAttribute("data-sehir");
                if (!regionMatchesListSlug(dataSehir, d.cityValue())) {
                    continue;
                }

                List<WebElement> sessions = block.findElements(By.cssSelector(DETAIL_SESSION_BLOCK));
                for (WebElement session : sessions) {
                    List<WebElement> times = session.findElements(By.cssSelector(DETAIL_START_DATE));
                    if (times.isEmpty()) {
                        continue;
                    }
                    String content = times.get(0).getAttribute("content");
                    if (content == null || content.isBlank()) {
                        continue;
                    }
                    LocalDateTime ldt;
                    try {
                        ldt = LocalDateTime.parse(content);
                    } catch (Exception e) {
                        logger.fine("Biletinial detay startDate parse edilemedi: " + content);
                        continue;
                    }
                    if (!ldt.toLocalDate().equals(d.listDate())) {
                        continue;
                    }

                    String venue = d.venueName();
                    try {
                        WebElement addr = session.findElement(By.cssSelector(DETAIL_VENUE_ADDRESS));
                        String v = addr.getText();
                        if (v != null && !v.trim().isEmpty()) {
                            venue = v.trim();
                        }
                    } catch (Exception ignored) {
                    }

                    String raw = times.get(0).getText();
                    if (raw == null || raw.isBlank()) {
                        raw = ldt.toString();
                    }

                    String key = d.ticketUrl() + "|" + content;
                    if (!seenSessionKeys.add(key)) {
                        continue;
                    }

                    EventType eventType = determineEventType(d.title(), d.category());
                    out.add(ScrapedEventData.builder()
                            .title(d.title())
                            .rawDateString(raw)
                            .startDate(ldt.toLocalDate())
                            .venueName(venue)
                            .location(venue)
                            .city(ScrapedEventData.normalizeCity(d.cityLabel()))
                            .ticketUrl(d.ticketUrl())
                            .imageUrl(d.imageUrl())
                            .eventType(eventType)
                            .source(EventSourceType.BILETINIAL)
                            .build());
                }
            }
        } catch (Exception e) {
            logger.log(Level.FINE, "Biletinial detay okunamadı: " + d.ticketUrl() + " — " + e.getMessage());
        }
        return out;
    }

    private ScrapedEventData buildFromDraftListOnly(CardDraft d) {
        EventType eventType = determineEventType(d.title(), d.category());
        return ScrapedEventData.builder()
                .title(d.title())
                .rawDateString(d.listDate().toString())
                .startDate(d.listDate())
                .venueName(d.venueName())
                .location(d.venueName())
                .city(ScrapedEventData.normalizeCity(d.cityLabel()))
                .ticketUrl(d.ticketUrl())
                .imageUrl(d.imageUrl())
                .eventType(eventType)
                .source(EventSourceType.BILETINIAL)
                .build();
    }

    /**
     * Liste URL'sindeki şehir segmenti (istanbul-anadolu, ankara, …) ile detaydaki data-sehir eşlemesi.
     */
    private boolean regionMatchesListSlug(String dataSehir, String citySlug) {
        if (dataSehir == null || citySlug == null) {
            return false;
        }
        String dn = normalizeForMatch(dataSehir);
        String slug = citySlug.trim().toLowerCase(Locale.ROOT);
        if (slug.equals("istanbul-anadolu")) {
            return dn.contains("anadolu");
        }
        if (slug.equals("istanbul-avrupa")) {
            return dn.contains("avrupa");
        }
        return dn.contains(normalizeForMatch(slug.replace('-', ' ')))
                || dn.contains(slug.replace("-", ""));
    }

    private static String normalizeForMatch(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().toLowerCase(Locale.forLanguageTag("tr"))
                .replace('ı', 'i')
                .replace(" ", "");
    }

    private EventType determineEventType(String title, BiletinialCategory category) {
        String lowerTitle = title.toLowerCase(Locale.forLanguageTag("tr"));
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

    private record CardDraft(
            String title,
            String ticketUrl,
            String imageUrl,
            String venueName,
            String cityLabel,
            BiletinialCategory category,
            String cityValue,
            LocalDate listDate
    ) {
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
