package com.asaf.whatnext.scraper;

import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.scraper.web.HtmlFetcher;
import com.asaf.whatnext.services.ScrapeHealthService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * BUGECE (bugece.co): sitenin kendi belgelediği açık liste API'si ({@code /api/openapi.json}),
 * robots.txt {@code /api/list}'e açıkça izin veriyor. Envanter çoğunlukla elektronik/kulüp gecesi —
 * diğer kaynakların neredeyse hiç kapsamadığı tür. Hepsi müzik etkinliği → CONCERT.
 */
@Service
@Order(50)
public class BugeceScraper extends BaseScraper {

    static final String BASE_URL = "https://bugece.co";
    private static final String LIST_URL = BASE_URL + "/api/list?indices=events&city=%s&page=%d&pageSize=%d";
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 20;
    private static final ZoneId TR = ZoneId.of("Europe/Istanbul");

    private final ScrapeHealthService scrapeHealthService;
    private final double minParseRatio;

    public BugeceScraper(ScrapeHealthService scrapeHealthService,
                         @Value("${whatnext.scrape-health.min-parse-ratio:0.8}") double minParseRatio) {
        super(BugeceScraper.class);
        this.scrapeHealthService = scrapeHealthService;
        this.minParseRatio = minParseRatio;
    }

    @Override
    public List<ScrapedEventData> scrapeEvents(String city) {
        String cityKey = city == null || city.isBlank() ? getDefaultCity() : ScrapedEventData.normalizeCity(city);
        logger.info("Scraping events from Bugece for city: " + cityKey);
        ScrapeRunStats stats = new ScrapeRunStats(getSourceName());
        List<ScrapedEventData> events = new ArrayList<>();

        for (int page = 1; page <= MAX_PAGES; page++) {
            JsonNode data;
            try {
                data = HtmlFetcher.json(HttpRequest.newBuilder(URI.create(String.format(LIST_URL,
                                URLEncoder.encode(cityKey, StandardCharsets.UTF_8), page, PAGE_SIZE)))
                        .header("User-Agent", HtmlFetcher.USER_AGENT)
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build()).path("data");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Bugece liste isteği başarısız (sayfa " + page + ")", e);
                break;
            }
            JsonNode hits = data.path("hits");
            for (JsonNode hit : hits) {
                stats.recordCardSeen();
                ScrapedEventData d = toEventData(hit);
                if (d == null) {
                    stats.recordParseFailed(hit.path("slug").asText(""));
                    continue;
                }
                events.add(d);
                stats.recordParsed(d.getTitle());
                if (d.getVenueName() == null) {
                    stats.recordMissingVenue(d.getTitle());
                }
                if (d.getMinPriceTl() != null) {
                    stats.recordPriceExtracted();
                }
            }
            if (hits.size() < PAGE_SIZE || page * PAGE_SIZE >= data.path("totalHits").asInt(0)) {
                break;
            }
        }

        logger.info("Scraped " + events.size() + " events from Bugece");
        finishAndRecordStats(stats, scrapeHealthService, minParseRatio);
        return events;
    }

    /** Liste API'sinin tek kaydı → {@link ScrapedEventData}; başlık/slug/tarih yoksa {@code null}. */
    static ScrapedEventData toEventData(JsonNode hit) {
        String title = hit.path("name").asText("").trim();
        String slug = hit.path("slug").asText("");
        ZonedDateTime start = instant(hit.path("start_time").asText(hit.path("date").asText("")));
        if (title.isEmpty() || slug.isEmpty() || start == null) {
            return null;
        }
        ZonedDateTime end = instant(hit.path("end_time").asText(""));
        String venue = hit.path("venue").path("name").asText("").trim();
        String city = hit.path("city").path("name").asText("");
        String image = hit.path("image").asText("");
        JsonNode price = hit.path("starting_from");

        return ScrapedEventData.builder()
                .title(title)
                .rawDateString(start.toString())
                .startDate(start.toLocalDate())
                .startTime(start.toLocalTime())
                // Gece etkinlikleri ertesi sabah biter; yalnız çok günlü (festival) ise bitiş günü yazılır.
                .endDate(end == null || !end.toLocalDate().isAfter(start.toLocalDate().plusDays(1))
                        ? null : end.toLocalDate())
                .venueName(venue.isEmpty() ? null : venue)
                .venueSlug(hit.path("venue").path("slug").asText(null))
                .location(city)
                .city(ScrapedEventData.normalizeCity(city))
                .ticketUrl(BASE_URL + "/tr/event/" + slug)
                .imageUrl(image.isEmpty() ? null : image)
                .minPriceTl(price.isNumber() && price.asInt() > 0 ? price.asInt() : null)
                .eventType(EventType.CONCERT)
                .source(EventSourceType.BUGECE)
                .sourcePopularity(popularityOf(hit))
                .build();
    }

    /** Sitenin kendi vitrin bölümleri: çok satan/TOP 10 → 20, editör seçkisi → 10 (Biletix ölçeği 0–20). */
    static int popularityOf(JsonNode hit) {
        int points = 0;
        for (JsonNode section : hit.path("sections")) {
            String s = section.path("slug").asText("");
            if (s.startsWith("top-10") || s.startsWith("gunun-en-cok-satan")) {
                return 20;
            }
            if (s.startsWith("editorun-sectikleri")) {
                points = 10;
            }
        }
        return points;
    }

    private static ZonedDateTime instant(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(raw).atZone(TR);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getSourceName() {
        return "Bugece";
    }

    @Override
    public String getSourceUrl() {
        return BASE_URL;
    }

    @Override
    public EventSourceType getEventSourceType() {
        return EventSourceType.BUGECE;
    }

    @Override
    protected String getDefaultCity() {
        return "istanbul";
    }
}
