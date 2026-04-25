package com.asaf.whatnext.scraper;

import com.asaf.whatnext.config.BubiletScraperConfig;
import com.asaf.whatnext.enums.Bubilet.BubiletCategory;
import com.asaf.whatnext.enums.Bubilet.BubiletCity;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.scraper.bubilet.BubiletCardDraft;
import com.asaf.whatnext.scraper.bubilet.BubiletDateRules;
import com.asaf.whatnext.scraper.bubilet.BubiletDetailParser;
import com.asaf.whatnext.scraper.bubilet.BubiletEventMapper;
import com.asaf.whatnext.scraper.bubilet.BubiletListCardParser;
import com.asaf.whatnext.scraper.common.DateScrapeStrategy;
import com.asaf.whatnext.utils.Texts;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
@Service
@Order(10)
public class BubiletScraper extends BaseScraper {

    private final BubiletScraperConfig config;
    private final BubiletListCardParser cardParser = new BubiletListCardParser();
    private final boolean enrichMissingVenueFromDetail;
    private final int listDateRolloverDays;
    private final int biletlerSectionRolloverDays;
    private final int draftProgressLogEvery;

    @Autowired
    public BubiletScraper(
            BubiletScraperConfig config,
            @Value("${bubilet.enrich-missing-venue-from-detail:false}") boolean enrichMissingVenueFromDetail,
            @Value("${bubilet.list-date.rollover-days:30}") int listDateRolloverDays,
            @Value("${bubilet.biletler-date.rollover-days:60}") int biletlerSectionRolloverDays,
            @Value("${bubilet.draft.progress-log-every:25}") int draftProgressLogEvery) {
        super(config.getChromePath(), config.getWaitTimeoutSeconds(), BubiletScraper.class);
        this.config = config;
        this.enrichMissingVenueFromDetail = enrichMissingVenueFromDetail;
        this.listDateRolloverDays = listDateRolloverDays;
        this.biletlerSectionRolloverDays = biletlerSectionRolloverDays;
        this.draftProgressLogEvery = draftProgressLogEvery;
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
            logger.info("Bubilet kategori " + category + ": liste sayfası yükleniyor…");
            WebScraperUtils.navigateToUrl(driver, wait, searchUrl);
            WebScraperUtils.handleCookieConsent(driver);
            WebScraperUtils.handleOverlays(driver);

            long t0 = System.currentTimeMillis();
            WebScraperUtils.scrollToLoadAll(
                driver,
                config.getMaxScrollAttempts(),
                config.getScrollWaitMs(),
                BubiletScraperConfig.EVENT_CARD_SELECTOR
            );
            logger.info("Bubilet kategori " + category + ": kaydırma bitti, süre " + (System.currentTimeMillis() - t0) + " ms");

            List<WebElement> cards = driver.findElements(By.cssSelector(BubiletScraperConfig.EVENT_CARD_SELECTOR));
            if (cards.isEmpty()) {
                logger.warn("Bubilet: kart bulunamadı (selector veya sayfa yapısı değişmiş olabilir): " + searchUrl);
            } else {
                logger.info("Bubilet kategori " + category + ": " + cards.size() + " kart; detay/expand aşamasına geçiliyor");
            }

            List<BubiletCardDraft> drafts = new ArrayList<>();
            for (WebElement card : cards) {
                try {
                    BubiletCardDraft d = extractCardDraft(card, city.getValue());
                    if (d != null) {
                        drafts.add(d);
                    }
                } catch (Exception e) {
                    logger.debug("Error reading list card", e);
                }
            }

            if (!drafts.isEmpty()) {
                logger.info("Bubilet kategori " + category + ": " + drafts.size()
                        + " taslak genişletiliyor (çok günlü detay / tek günlü mekân tazeleme uzun sürebilir)");
            }
            int draftIndex = 0;
            for (BubiletCardDraft d : drafts) {
                try {
                    draftIndex++;
                    if (drafts.size() > 10
                            && (draftIndex == 1
                            || draftIndex % draftProgressLogEvery == 0
                            || draftIndex == drafts.size())) {
                        logger.info("Bubilet kategori " + category + ": taslak " + draftIndex + "/" + drafts.size() + " — " + d.title());
                    }
                    events.addAll(expandDraftToEvents(
                            d,
                            category,
                            new BubiletDetailParser(driver, wait, biletlerSectionRolloverDays)));
                } catch (Exception e) {
                    logger.debug("Error expanding card: " + d.title(), e);
                }
            }

        } catch (Exception e) {
            logger.error("Error scraping category: " + category.name(), e);
        }

        return events;
    }

    /**
     * Liste kartından ham veri (WebElement stale olmadan önce toplanır).
     */
    private BubiletCardDraft extractCardDraft(WebElement card, String city) {
        var parsed = cardParser.parse(card.getAttribute("outerHTML"));
        if (parsed.isEmpty()) {
            return null;
        }
        var p = parsed.get();
        String title = p.title();
        String dateText = p.dateText() != null ? p.dateText() : "";
        String venueName = p.venueName() != null ? p.venueName() : "";
        if (venueName.isEmpty()) {
            logger.debug("Venue not found for event: " + title);
        }
        if (dateText.isEmpty()) {
            logger.debug("Date not found for event: " + title);
        }

        return new BubiletCardDraft(title.trim(), p.ticketUrl(), dateText.trim(), p.imageUrl(), city, venueName);
    }

    /**
     * Tek tarih: listeden parse. Çok tarih: detay sayfası #biletler-section içinden seans tarihleri.
     */
    private List<ScrapedEventData> expandDraftToEvents(
            BubiletCardDraft d,
            BubiletCategory category,
            BubiletDetailParser detailParser) {
        EventType eventType = BubiletEventMapper.toEventType(category);

        if (BubiletDateRules.toStrategy(d.dateText()) == DateScrapeStrategy.FROM_LIST_ONLY) {
            LocalDate parsed = BubiletEventMapper.parseListDateLine(d.dateText(), listDateRolloverDays);
            BubiletCardDraft dEnriched = withDetailVenueIfListBlank(d, detailParser);
            return List.of(BubiletEventMapper.toScraped(dEnriched, parsed, d.dateText(), eventType));
        }

        BubiletDetailParser.MultiDateDetail detail = detailParser.loadMultiDate(d.ticketUrl());
        if (detail.dates().isEmpty()) {
            logger.debug("Çok tarihli kart, detaydan tarih çıkmadı: " + d.title() + " (" + d.dateText() + ")");
            return List.of();
        }
        String mergedVenue = Texts.firstNonBlank(d.venueName(), detail.venue());
        BubiletCardDraft dUse = new BubiletCardDraft(
                d.title(), d.ticketUrl(), d.dateText(), d.imageUrl(), d.city(), mergedVenue);

        List<ScrapedEventData> out = new ArrayList<>();
        for (LocalDate ld : detail.dates()) {
            out.add(BubiletEventMapper.toScraped(dUse, ld, ld.toString(), eventType));
        }
        return out;
    }

    /**
     * Liste satırında mekân yoksa etkinlik detayından doldur (tek günlü kartlarda aynı detay sayfası).
     */
    private BubiletCardDraft withDetailVenueIfListBlank(BubiletCardDraft d, BubiletDetailParser detailParser) {
        if (d.venueName() != null && !d.venueName().isBlank()) {
            return d;
        }
        if (!enrichMissingVenueFromDetail) {
            return d;
        }
        String v = detailParser.loadVenueForSingleDate(d.ticketUrl());
        if (v == null || v.isBlank()) {
            return d;
        }
        return new BubiletCardDraft(
                d.title(), d.ticketUrl(), d.dateText(), d.imageUrl(), d.city(), v);
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
