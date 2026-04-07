package com.asaf.whatnext.scraper;

import com.asaf.whatnext.config.BubiletScraperConfig;
import com.asaf.whatnext.enums.Bubilet.BubiletCategory;
import com.asaf.whatnext.enums.Bubilet.BubiletCity;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.utils.WebScraperUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BubiletScraper extends BaseScraper {

    private static final Pattern TR_DATE_IN_TEXT = Pattern.compile(
            "(\\d{1,2})\\s+(Ocak|Şubat|Mart|Nisan|Mayıs|Haziran|Temmuz|Ağustos|Eylül|Ekim|Kasım|Aralık)");

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
        allData = dedupeByPathAndDate(allData);
        logger.info("Scraped " + allData.size() + " events from Bubilet (yol+tarih tekilleştirilmiş)");

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

            List<CardDraft> drafts = new ArrayList<>();
            for (WebElement card : cards) {
                try {
                    CardDraft d = extractCardDraft(card, city.getValue());
                    if (d != null) {
                        drafts.add(d);
                    }
                } catch (Exception e) {
                    logger.log(Level.FINE, "Error reading list card", e);
                }
            }

            for (CardDraft d : drafts) {
                try {
                    events.addAll(expandDraftToEvents(d, category));
                } catch (Exception e) {
                    logger.log(Level.FINE, "Error expanding card: " + d.title(), e);
                }
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error scraping category: " + category.name(), e);
        }

        return events;
    }

    /**
     * Liste kartından ham veri (WebElement stale olmadan önce toplanır).
     */
    private CardDraft extractCardDraft(WebElement card, String city) {
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

        String imageUrl = null;
        try {
            imageUrl = card.findElement(By.cssSelector(BubiletScraperConfig.EVENT_IMAGE_SELECTOR)).getAttribute("src");
        } catch (Exception ignored) {
        }

        return new CardDraft(title.trim(), ticketUrl, dateText.trim(), imageUrl, city);
    }

    /**
     * Tek tarih: listeden parse. Çok tarih: detay sayfası #biletler-section içinden seans tarihleri.
     */
    private List<ScrapedEventData> expandDraftToEvents(CardDraft d, BubiletCategory category) {
        EventType eventType = determineEventType(category);

        if (isSingleDateEvent(d.dateText())) {
            LocalDate parsed = parseDate(d.dateText());
            return List.of(buildScraped(d, parsed, d.dateText(), eventType));
        }

        List<LocalDate> dates = fetchDatesFromDetailPage(d.ticketUrl());
        if (dates.isEmpty()) {
            logger.fine("Çok tarihli kart, detaydan tarih çıkmadı: " + d.title() + " (" + d.dateText() + ")");
            return List.of();
        }

        List<ScrapedEventData> out = new ArrayList<>();
        for (LocalDate ld : dates) {
            out.add(buildScraped(d, ld, ld.toString(), eventType));
        }
        return out;
    }

    private ScrapedEventData buildScraped(CardDraft d, LocalDate startDate, String rawDate, EventType eventType) {
        return ScrapedEventData.builder()
                .title(d.title())
                .rawDateString(rawDate)
                .startDate(startDate)
                .ticketUrl(d.ticketUrl())
                .imageUrl(d.imageUrl())
                .eventType(eventType)
                .source(EventSourceType.BUBILET)
                .city(ScrapedEventData.normalizeCity(d.city()))
                .build();
    }

    private List<LocalDate> fetchDatesFromDetailPage(String url) {
        try {
            WebScraperUtils.navigateToUrl(driver, wait, url);
            WebScraperUtils.handleCookieConsent(driver);
            WebScraperUtils.handleOverlays(driver);

            wait.until(ExpectedConditions.presenceOfElementLocated(By.id(BubiletScraperConfig.DETAIL_BILETLER_SECTION_ID)));
            WebElement section = driver.findElement(By.id(BubiletScraperConfig.DETAIL_BILETLER_SECTION_ID));
            return extractDatesFromBiletlerText(section.getText());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Bubilet detay sayfası tarihleri okunamadı: " + url + " — " + e.getMessage());
            return List.of();
        }
    }

    /**
     * "Biletler" bölümündeki metinden (28 Nisan Sal, 12 Haziran Cum, …) tarih çıkarır.
     */
    private List<LocalDate> extractDatesFromBiletlerText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        Matcher m = TR_DATE_IN_TEXT.matcher(text);
        TreeSet<LocalDate> unique = new TreeSet<>();
        while (m.find()) {
            int day = Integer.parseInt(m.group(1));
            int month = monthNumberOrZero(m.group(2));
            if (month < 1) {
                continue;
            }
            int year = LocalDate.now().getYear();
            try {
                LocalDate date = LocalDate.of(year, month, day);
                if (date.isBefore(LocalDate.now().minusDays(60))) {
                    date = date.plusYears(1);
                }
                unique.add(date);
            } catch (Exception ignored) {
            }
        }
        return new ArrayList<>(unique);
    }

    private int monthNumberOrZero(String monthStr) {
        return switch (monthStr) {
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
            default -> 0;
        };
    }

    private record CardDraft(String title, String ticketUrl, String dateText, String imageUrl, String city) {}

    /**
     * Liste kartındaki tek satırlık tarih: çoklu gün ("17 Nisan, 19 Nisan ...") veya "..." ile kesilen seriler → detay.
     * Aynı gün iki seans ("22 Nisan Çar - 19:00, 21:30") kabul — içinde " - " vardır.
     */
    private boolean isSingleDateEvent(String dateText) {
        if (dateText == null || dateText.isEmpty()) {
            return false;
        }
        String t = dateText.trim();
        if (t.contains("...")) {
            return false;
        }
        if (t.contains(",") && !t.contains(" - ")) {
            return false;
        }
        return true;
    }

    /** Aynı etkinlik slug'ı farklı seans tarihleriyle ayrı kayıt. */
    private List<ScrapedEventData> dedupeByPathAndDate(List<ScrapedEventData> items) {
        Map<String, ScrapedEventData> m = new LinkedHashMap<>();
        for (ScrapedEventData d : items) {
            if (d == null || d.getTicketUrl() == null) {
                continue;
            }
            String path = canonicalEtkinlikPath(d.getTicketUrl());
            if (path.isEmpty()) {
                continue;
            }
            String key = path + "|" + (d.getStartDate() != null ? d.getStartDate().toString() : "");
            m.putIfAbsent(key, d);
        }
        return new ArrayList<>(m.values());
    }

    private static String canonicalEtkinlikPath(String ticketUrl) {
        if (ticketUrl == null) {
            return "";
        }
        int i = ticketUrl.indexOf("/etkinlik/");
        if (i < 0) {
            return ticketUrl.trim().toLowerCase(Locale.ROOT);
        }
        String path = ticketUrl.substring(i);
        int q = path.indexOf('?');
        if (q >= 0) {
            path = path.substring(0, q);
        }
        return path.toLowerCase(Locale.ROOT);
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

                int month = monthNumberOrZero(monthStr);
                if (month < 1) {
                    return null;
                }

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
