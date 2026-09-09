package com.asaf.whatnext.scraper;

import com.asaf.whatnext.config.BubiletScraperConfig;
import com.asaf.whatnext.scraper.web.HtmlFetcher;
import com.asaf.whatnext.enums.AudienceType;
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
import com.asaf.whatnext.services.ScrapeHealthService;
import com.asaf.whatnext.utils.Texts;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
@Service
@Order(10)
public class BubiletScraper extends BaseScraper {

    private final BubiletScraperConfig config;
    private final BubiletListCardParser cardParser = new BubiletListCardParser();
    private final boolean enrichMissingVenueFromDetail;
    private final int listDateRolloverDays;
    private final int biletlerSectionRolloverDays;
    private final int draftProgressLogEvery;
    private final ScrapeHealthService scrapeHealthService;
    private final double minParseRatio;
    private int unknownCityCount = 0;

    @Autowired
    public BubiletScraper(
            BubiletScraperConfig config,
            ScrapeHealthService scrapeHealthService,
            @Value("${bubilet.enrich-missing-venue-from-detail:true}") boolean enrichMissingVenueFromDetail,
            @Value("${bubilet.list-date.rollover-days:30}") int listDateRolloverDays,
            @Value("${bubilet.biletler-date.rollover-days:60}") int biletlerSectionRolloverDays,
            @Value("${bubilet.draft.progress-log-every:25}") int draftProgressLogEvery,
            @Value("${bubilet.scrape-health.min-parse-ratio:${whatnext.scrape-health.min-parse-ratio:0.8}}") double minParseRatio) {
        super(null, 0, BubiletScraper.class);
        this.config = config;
        this.scrapeHealthService = scrapeHealthService;
        this.enrichMissingVenueFromDetail = enrichMissingVenueFromDetail;
        this.listDateRolloverDays = listDateRolloverDays;
        this.biletlerSectionRolloverDays = biletlerSectionRolloverDays;
        this.draftProgressLogEvery = draftProgressLogEvery;
        this.minParseRatio = minParseRatio;
    }

    @Override
    public List<ScrapedEventData> scrapeEvents(String city) {
        logger.info("Scraping events from Bubilet for city: " + city);

        ScrapeRunStats stats = new ScrapeRunStats("Bubilet");
        BubiletCity selectedCity = parseCity(city, stats);

        Map<BubiletCategory, List<ScrapedEventData>> byCategory = new EnumMap<>(BubiletCategory.class);
        List<ScrapedEventData> allData = new ArrayList<>();
        for (BubiletCategory category : Arrays.asList(
                BubiletCategory.KIDS, BubiletCategory.THEATRE, BubiletCategory.MUSIC, BubiletCategory.STANDUP)) {
            List<ScrapedEventData> categoryData = scrapeCategory(category, selectedCity, stats);
            byCategory.put(category, categoryData);
            allData.addAll(categoryData);
        }

        markChildrenAudience(byCategory, allData);
        allData = dedupeByPathAndDate(allData);
        logger.info("Scraped " + allData.size() + " events from Bubilet (yol+tarih tekilleştirilmiş)");

        long validCount = allData.stream().filter(ScrapedEventData::isValid).count();
        logger.info("Valid events: " + validCount + "/" + allData.size());

        finishAndRecordStats(stats, scrapeHealthService, minParseRatio);

        return allData;
    }

    private List<ScrapedEventData> scrapeCategory(BubiletCategory category, BubiletCity city, ScrapeRunStats stats) {
        List<ScrapedEventData> events = new ArrayList<>();

        try {
            String searchUrl = config.buildSearchUrl(category, city);
            logger.info("Bubilet kategori " + category + ": liste sayfası yükleniyor…");

            // Liste sunucuda render ediliyor: kartların tamamı tek GET yanıtında geliyor
            // (2026-09-09 canlı doğrulama: tiyatro/İstanbul 48/48), kaydırmaya gerek yok.
            List<Element> cards = HtmlFetcher.get(searchUrl, config.getTimeoutMs())
                    .select(BubiletScraperConfig.EVENT_CARD_SELECTOR);
            if (cards.isEmpty()) {
                logger.warn("Bubilet: kart bulunamadı (selector veya sayfa yapısı değişmiş olabilir): " + searchUrl);
            } else {
                logger.info("Bubilet kategori " + category + ": " + cards.size() + " kart; detay/expand aşamasına geçiliyor");
            }

            List<BubiletCardDraft> drafts = new ArrayList<>();
            for (Element card : cards) {
                stats.recordCardSeen();
                try {
                    BubiletCardDraft d = extractCardDraft(card, city.getValue());
                    if (d != null) {
                        drafts.add(d);
                        stats.recordParsed(d.title());
                        if (d.venueName() == null || d.venueName().isBlank()) {
                            stats.recordMissingVenue(d.title());
                        }
                        if (d.dateText() == null || d.dateText().isBlank()) {
                            stats.recordMissingDate(d.title());
                        }
                        if (d.multiVenue()) {
                            stats.recordMultiVenue(d.title());
                        }
                        if (d.minPriceTl() != null) {
                            stats.recordPriceExtracted();
                        }
                    } else {
                        stats.recordParseFailed(safeSnippet(card));
                    }
                } catch (Exception e) {
                    stats.recordParseFailed(safeSnippet(card));
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
                            new BubiletDetailParser(biletlerSectionRolloverDays, config.getTimeoutMs())));
                } catch (Exception e) {
                    logger.debug("Error expanding card: " + d.title(), e);
                }
            }

        } catch (Exception e) {
            logger.error("Error scraping category: " + category.name(), e);
        }

        return events;
    }

    /** {@link CategoryScraper} imzasını bozmadan kategori sonucunu yan etki olarak yakalar (KIDS çapraz-işaretleme için). */
    private static List<ScrapedEventData> capture(
            Map<BubiletCategory, List<ScrapedEventData>> byCategory, BubiletCategory category, List<ScrapedEventData> data) {
        byCategory.put(category, data);
        return data;
    }

    /**
     * KIDS etiketinden gelenler doğrudan CHILDREN; diğer kategorilerde aynı (city+path) etkinlik varsa
     * ("aynı oyun iki etikette de listeleniyor") o da CHILDREN işaretlenir.
     */
    private void markChildrenAudience(Map<BubiletCategory, List<ScrapedEventData>> byCategory, List<ScrapedEventData> allData) {
        List<ScrapedEventData> kidsEvents = byCategory.getOrDefault(BubiletCategory.KIDS, List.of());
        kidsEvents.forEach(d -> d.setAudience(AudienceType.CHILDREN));

        Set<String> kidsKeys = kidsEvents.stream()
                .map(BubiletScraper::cityPathKey)
                .collect(Collectors.toSet());
        for (ScrapedEventData d : allData) {
            if (d.getAudience() == null && kidsKeys.contains(cityPathKey(d))) {
                d.setAudience(AudienceType.CHILDREN);
            }
        }
    }

    private static String cityPathKey(ScrapedEventData d) {
        return (d.getCity() != null ? d.getCity() : "") + "|" + canonicalEtkinlikPath(d.getTicketUrl());
    }

    private static String safeSnippet(Element card) {
        String html = card.outerHtml();
        return html.substring(0, Math.min(120, html.length()));
    }

    /**
     * Liste kartından ham veri (WebElement stale olmadan önce toplanır).
     */
    private BubiletCardDraft extractCardDraft(Element card, String city) {
        var parsed = cardParser.parse(card.outerHtml());
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

        return new BubiletCardDraft(
                title.trim(), p.ticketUrl(), dateText.trim(), p.imageUrl(), city, venueName,
                p.venueSlug(), p.minPriceTl(), p.multiVenue(), p.venueNames());
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
                d.title(), d.ticketUrl(), d.dateText(), d.imageUrl(), d.city(), mergedVenue,
                d.venueSlug(), d.minPriceTl(), d.multiVenue(), d.venueNames());

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
                d.title(), d.ticketUrl(), d.dateText(), d.imageUrl(), d.city(), v,
                d.venueSlug(), d.minPriceTl(), d.multiVenue(), d.venueNames());
    }

    /**
     * Aynı etkinlik slug'ı farklı seans tarihleriyle ayrı kayıt.
     * Anahtara şehir eklenir: {@code canonicalEtkinlikPath} URL'den {@code /{city}/} önekini atıyor,
     * bu yüzden şehir olmadan İstanbul+Ankara turnesi aynı slug'a çöküp bir şehri düşürüyordu.
     */
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
            String city = d.getCity() != null ? d.getCity() : "";
            String key = city + "|" + path + "|" + (d.getStartDate() != null ? d.getStartDate().toString() : "");
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

    private BubiletCity parseCity(String city, ScrapeRunStats stats) {
        Optional<BubiletCity> match = Arrays.stream(BubiletCity.values())
                .filter(c -> c.getValue().equalsIgnoreCase(city))
                .findFirst();
        if (match.isEmpty()) {
            unknownCityCount++;
            stats.recordUnknownCity();
            logger.warn("Bubilet: bilinmeyen şehir '" + city + "', İstanbul'a düşülüyor (unknownCityCount="
                    + unknownCityCount + ")");
            return BubiletCity.ISTANBUL;
        }
        return match.get();
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
