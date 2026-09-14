package com.asaf.whatnext.scraper.biletix;

import com.asaf.whatnext.config.BiletixScraperConfig;
import com.asaf.whatnext.enums.Biletix.BiletixCategory;
import com.asaf.whatnext.enums.Biletix.BiletixCity;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.scraper.ScrapedEventData;
import com.asaf.whatnext.scraper.web.HtmlFetcher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Biletix arama sonuçlarını sitenin kendi Solr uç noktasından çeker.
 *
 * <p>Şehir filtresi burada {@code fq=region:...} ile uygulanır: sitenin kendi isteği region
 * göndermediğinden DOM'da şehir dışı etkinlikler karışıyordu ve kartta şehir alanı olmadığı için
 * ayıklanamıyordu. Solr belgesi {@code city}/{@code region} taşıdığından şehir artık kesin.
 */
public class BiletixSolrClient {

    private static final ZoneId TR = ZoneId.of("Europe/Istanbul");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient http;
    private final int rows;
    private final Duration timeout;

    public BiletixSolrClient(int rows, int timeoutSeconds) {
        this.rows = rows;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    /** Verilen kategori+şehir için Solr belgeleri (yalnız {@code type:event}, gruplar hariç). */
    public List<JsonNode> fetchDocs(BiletixCategory category, BiletixCity city) throws IOException, InterruptedException {
        String body = form(
                "q", "*:*",
                "fq", BiletixScraperConfig.buildSubcategoryFilter(category),
                "fq", "region:" + BiletixScraperConfig.regionOf(city),
                "fq", "type:event",
                "rows", String.valueOf(rows),
                "sort", "start asc",
                "wt", "json");

        HttpRequest request = HttpRequest.newBuilder(URI.create(BiletixScraperConfig.SOLR_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("Biletix solr HTTP " + response.statusCode());
        }

        List<JsonNode> docs = new ArrayList<>();
        MAPPER.readTree(response.body()).path("response").path("docs").forEach(docs::add);
        return docs;
    }

    /** Düz GET + JSON (ana sayfa listeleri, bkz. {@link BiletixScraperConfig#HOMEPAGE_API_URL}). */
    public JsonNode fetchJson(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", HtmlFetcher.USER_AGENT)
                .timeout(timeout)
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("Biletix HTTP " + response.statusCode() + " — " + url);
        }
        return MAPPER.readTree(response.body());
    }

    /** Solr belgesini {@link ScrapedEventData}'ya çevirir; zorunlu alanlar eksikse {@code null}. */
    public static ScrapedEventData toEventData(JsonNode doc, BiletixCategory category) {
        String title = text(doc, "sname");
        String id = text(doc, "id");
        String region = text(doc, "region");
        if (title.isEmpty() || id.isEmpty() || region.isEmpty()) {
            return null;
        }

        ZonedDateTime start = instantAt(doc, "start");
        ZonedDateTime end = instantAt(doc, "end");
        String city = doc.path("city").path(0).asText("");
        String image = text(doc, "image_url");

        return ScrapedEventData.builder()
                .title(title)
                .rawDateString(text(doc, "start"))
                .startDate(start == null ? null : start.toLocalDate())
                .startTime(start == null ? null : start.toLocalTime())
                .endDate(end == null || start == null || end.toLocalDate().equals(start.toLocalDate())
                        ? null : end.toLocalDate())
                .venueName(text(doc, "svenue").trim())
                .location(city)
                .city(ScrapedEventData.normalizeCity(city))
                .ticketUrl(String.format(BiletixScraperConfig.EVENT_URL_FORMAT, id, region))
                .imageUrl(image.isEmpty() ? null : BiletixScraperConfig.IMAGE_URL_PREFIX + image)
                .eventType(eventTypeOf(text(doc, "subcategory"), category))
                .source(EventSourceType.BILETIX)
                .build();
    }

    static EventType eventTypeOf(String subcategory, BiletixCategory category) {
        if (category == BiletixCategory.MUSIC) {
            return EventType.CONCERT;
        }
        String sub = subcategory.substring(0, Math.max(0, subcategory.indexOf('$')));
        switch (sub) {
            case "stand_up":
                return EventType.STANDUP;
            case "bale_dans":
            case "gosteri":
                return EventType.PERFORMING_ART;
            default:
                return EventType.THEATER;
        }
    }

    /** Solr tarihleri UTC ("2026-09-09T18:00:00Z"); kart saatleri Türkiye saatiyle gösterilir. */
    private static ZonedDateTime instantAt(JsonNode doc, String field) {
        String raw = text(doc, field);
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(raw).atZone(TR);
        } catch (Exception e) {
            return null;
        }
    }

    private static String text(JsonNode doc, String field) {
        JsonNode node = doc.path(field);
        return node.isArray() ? node.path(0).asText("") : node.asText("");
    }

    private static String form(String... pairs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pairs.length; i += 2) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(pairs[i], StandardCharsets.UTF_8))
              .append('=')
              .append(URLEncoder.encode(pairs[i + 1], StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
