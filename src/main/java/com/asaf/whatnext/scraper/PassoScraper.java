package com.asaf.whatnext.scraper;

import com.asaf.whatnext.enums.AudienceType;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.scraper.web.HtmlFetcher;
import com.asaf.whatnext.services.ScrapeHealthService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Passo: sitenin kendi Angular uygulamasının çağırdığı anonim JSON uç noktası
 * ({@code POST .../api/passoweb/allevents}). Zorlu PSM, Volkswagen Arena, Ülker, CRR gibi
 * yalnız Passo'da satılan büyük salonları getirir.
 *
 * <p>Kayıtta şehir ve tür alanı yok; site de öyle yapıyor: {@code CityId} (İstanbul = 101,
 * {@code getalleventlocation}) ve {@code SubGenreId} ({@code getallsubgenresbygenreid/{genre}/118})
 * gövdeye konup alt tür başına ayrı sorgu atılır. Futbol/Spor/Müze/Tur-Workshop alt türleri hiç sorulmaz.
 * Sezon/seri biletleri ({@code eventType != 0}) atlanır; her seans ayrı kayıt olarak gelir.
 */
@Service
@Order(60)
public class PassoScraper extends BaseScraper {

    static final String WEB_URL = "https://www.passo.com.tr";
    private static final String API_URL = "https://ticketingweb.passo.com.tr/api/passoweb/allevents";
    private static final String IMAGE_BASE = "https://image.passo.com.tr/api/r/tr/p/event/";
    private static final int TURKISH = 118;
    private static final int PAGE_SIZE = 200;
    private static final int MAX_PAGES = 15;
    private static final long REQUEST_GAP_MS = 1500;

    /** Şehir → Passo locationId ({@code getalleventlocation}, 2026-11-02). */
    private static final Map<String, Integer> CITY_IDS = Map.of("istanbul", 101, "ankara", 109, "izmir", 137, "antalya", 110);

    /** Passo alt türü → bizim tür. Sıra önemli değil; anahtar = SubGenreId. */
    static final Map<Integer, EventType> SUBGENRES = new LinkedHashMap<>();
    static {
        // Müzik (8615): Diğer, Pop-Rock, Festival, Heavy Metal, Dj-Party, Caz, Klasik
        for (int id : new int[]{12617, 13617, 14617, 38617, 41617, 42617, 56617}) {
            SUBGENRES.put(id, EventType.CONCERT);
        }
        // Sahne (11615)
        SUBGENRES.put(18617, EventType.THEATER);        // Tiyatro
        SUBGENRES.put(60617, EventType.THEATER);        // İKSV Tiyatro
        SUBGENRES.put(63617, EventType.THEATER);        // Diğer Sahne
        SUBGENRES.put(46617, EventType.THEATER);        // Çocuk
        SUBGENRES.put(44617, EventType.STANDUP);        // Stand Up
        SUBGENRES.put(40617, EventType.PERFORMING_ART); // Dans
        SUBGENRES.put(75617, EventType.PERFORMING_ART); // Bale
        SUBGENRES.put(77617, EventType.PERFORMING_ART); // Gösteri
        SUBGENRES.put(45617, EventType.PERFORMING_ART); // Müzikal (Müzik altında da aynı id)
        SUBGENRES.put(82617, EventType.PERFORMING_ART); // Müzikal Gösteri
        // Diğer (12615)
        SUBGENRES.put(78617, EventType.EXHIBITION);     // Sergi - Fuar
    }
    private static final Set<Integer> CHILDREN_SUBGENRES = Set.of(46617);

    private final ScrapeHealthService scrapeHealthService;
    private final double minParseRatio;

    public PassoScraper(ScrapeHealthService scrapeHealthService,
                        @Value("${whatnext.scrape-health.min-parse-ratio:0.8}") double minParseRatio) {
        super(PassoScraper.class);
        this.scrapeHealthService = scrapeHealthService;
        this.minParseRatio = minParseRatio;
    }

    @Override
    public List<ScrapedEventData> scrapeEvents(String city) {
        String cityKey = city == null || city.isBlank() ? getDefaultCity() : ScrapedEventData.normalizeCity(city);
        Integer cityId = CITY_IDS.get(cityKey);
        if (cityId == null) {
            logger.warn("Passo: bilinmeyen şehir " + city + ", atlanıyor");
            return List.of();
        }
        logger.info("Scraping events from Passo for city: " + cityKey);
        ScrapeRunStats stats = new ScrapeRunStats(getSourceName());
        Map<Long, ScrapedEventData> byId = new LinkedHashMap<>();

        for (Map.Entry<Integer, EventType> sub : SUBGENRES.entrySet()) {
            for (int page = 0; page < MAX_PAGES; page++) {
                JsonNode body;
                try {
                    body = fetchPage(cityId, sub.getKey(), page);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return finish(stats, byId);
                } catch (Exception e) {
                    logger.error("Passo isteği başarısız (alt tür " + sub.getKey() + ", sayfa " + page + ")", e);
                    break;
                }
                JsonNode items = body.path("valueList");
                for (JsonNode item : items) {
                    if (item.path("eventType").asInt(-1) != 0 || byId.containsKey(item.path("id").asLong())) {
                        continue; // seri/sezon bileti ya da başka alt türde zaten görüldü
                    }
                    stats.recordCardSeen();
                    ScrapedEventData d = toEventData(item, sub.getValue(), cityKey,
                            CHILDREN_SUBGENRES.contains(sub.getKey()));
                    if (d == null) {
                        stats.recordParseFailed(item.path("seoUrl").asText(""));
                        continue;
                    }
                    byId.put(item.path("id").asLong(), d);
                    stats.recordParsed(d.getTitle());
                    if (d.getVenueName() == null) {
                        stats.recordMissingVenue(d.getTitle());
                    }
                }
                if (items.size() < PAGE_SIZE || (page + 1) * PAGE_SIZE >= body.path("totalItemCount").asInt(0)) {
                    break;
                }
            }
        }
        return finish(stats, byId);
    }

    /**
     * Passo art arda gelen isteklere 429 dönüyor (2026-11-02: ~18 hızlı istekte). Her istekten önce
     * {@link #REQUEST_GAP_MS} beklenir; yine 429 gelirse 30 sn geri çekilip en fazla 2 kez yeniden denenir.
     */
    private JsonNode fetchPage(int cityId, int subGenreId, int page) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL))
                .header("User-Agent", HtmlFetcher.USER_AGENT)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(String.format(
                        "{\"LanguageId\":%d,\"CityId\":%d,\"SubGenreId\":%d,\"from\":%d,\"size\":%d}",
                        TURKISH, cityId, subGenreId, page * PAGE_SIZE, PAGE_SIZE)))
                .build();
        for (int attempt = 0; ; attempt++) {
            Thread.sleep(attempt == 0 ? REQUEST_GAP_MS : 30_000);
            try {
                return HtmlFetcher.json(request);
            } catch (IOException e) {
                if (attempt >= 2 || e.getMessage() == null || !e.getMessage().startsWith("HTTP 429")) {
                    throw e;
                }
                logger.info("Passo 429 (alt tür " + subGenreId + "), 30 sn bekleniyor");
            }
        }
    }

    private List<ScrapedEventData> finish(ScrapeRunStats stats, Map<Long, ScrapedEventData> byId) {
        logger.info("Scraped " + byId.size() + " events from Passo");
        finishAndRecordStats(stats, scrapeHealthService, minParseRatio);
        return new ArrayList<>(byId.values());
    }

    /** {@code allevents} kaydı → {@link ScrapedEventData}; ad/id/seoUrl/tarih yoksa {@code null}. */
    static ScrapedEventData toEventData(JsonNode item, EventType type, String city, boolean children) {
        String title = item.path("name").asText("").trim();
        long id = item.path("id").asLong(0);
        String seoUrl = item.path("seoUrl").asText("");
        LocalDateTime start = localDateTime(item.path("date").asText(""));
        if (title.isEmpty() || id == 0 || seoUrl.isEmpty() || start == null || start.getYear() < 2000) {
            return null;
        }
        LocalDateTime end = localDateTime(item.path("endDate").asText(""));
        String venue = cleanVenue(item.path("venueName").asText(""));
        String image = item.path("homePageImagePath").asText("");
        int q = image.indexOf('?');

        return ScrapedEventData.builder()
                .title(title)
                .rawDateString(item.path("date").asText(""))
                .startDate(start.toLocalDate())
                .startTime(item.path("hideHour").asBoolean(false) ? null : start.toLocalTime())
                .endDate(end == null || end.getYear() < 2000 || !end.toLocalDate().isAfter(start.toLocalDate())
                        ? null : end.toLocalDate())
                .venueName(venue)
                .location(city)
                .city(city)
                .ticketUrl(WEB_URL + "/tr/etkinlik/" + seoUrl + "/" + id)
                // ?w=760&h=400 webp küçük resim döndürür; sorgusuz adres orijinal JPEG.
                .imageUrl(image.isEmpty() ? null : IMAGE_BASE + (q < 0 ? image : image.substring(0, q)))
                .eventType(type)
                .audience(children ? AudienceType.CHILDREN : null)
                .source(EventSourceType.PASSO)
                .build();
    }

    /** "Zorlu PSM - İstanbul" / "Şerefiye Sarnıcı | İstanbul" → şehir eki atılır. */
    static String cleanVenue(String raw) {
        String v = raw == null ? "" : raw.replaceAll("\\s*[-|]\\s*İstanbul\\s*$", "").trim();
        return v.isEmpty() ? null : v;
    }

    private static LocalDateTime localDateTime(String raw) {
        try {
            return raw.isEmpty() ? null : LocalDateTime.parse(raw);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getSourceName() {
        return "Passo";
    }

    @Override
    public String getSourceUrl() {
        return WEB_URL;
    }

    @Override
    public EventSourceType getEventSourceType() {
        return EventSourceType.PASSO;
    }

    @Override
    protected String getDefaultCity() {
        return "istanbul";
    }
}
