package com.asaf.whatnext.services.scoring;

import com.asaf.whatnext.config.BiletixScraperConfig;
import com.asaf.whatnext.scraper.web.HtmlFetcher;
import com.asaf.whatnext.utils.EventDuplicateKeys;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Turne genişliği: bir isim önümüzdeki 90 günde Türkiye'de kaç farklı şehirde sahneye çıkıyor (Biletix, tüm
 * bölgeler). 2026-10-01: Redd (8 şehir), Hikayeden Adamlar (19) hafta sonu seçkisine giremiyor, tek şehirlik
 * küçük caz gecesi giriyordu — elle seçilmiş liste ve mekân puanı bilinen ismi ayırt edemiyordu.
 * Puanlama sırasında ağ çağrısı yapılmaz: indeks günde bir (ve ilk kullanımda bir kez) yenilenir, hata
 * olursa eldeki indeks korunur.
 */
@Component
public class TourIndex {

    private static final Logger LOGGER = LoggerFactory.getLogger(TourIndex.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_POINTS = 20;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    private volatile Map<String, Integer> citiesByName = Map.of();
    private volatile boolean loaded;

    /** Tek şehir 0; her ek şehir 2 puan, en fazla {@link #MAX_POINTS}. */
    public int score(String title) {
        if (!loaded) {
            refresh();
        }
        int cities = citiesByName.getOrDefault(key(title), 0);
        return Math.min(Math.max(cities - 1, 0) * 2, MAX_POINTS);
    }

    @Scheduled(cron = "0 30 4 * * *", zone = "Europe/Istanbul")
    public synchronized void refresh() {
        try {
            String body = "q=*:*&fq=type:event"
                    + "&fq=" + URLEncoder.encode("start:[NOW TO NOW+90DAYS]", StandardCharsets.UTF_8)
                    + "&rows=20000&fl=sname,city&wt=json";
            HttpRequest request = HttpRequest.newBuilder(URI.create(BiletixScraperConfig.SOLR_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = HtmlFetcher.send(http, request);
            if (response.statusCode() != 200) {
                throw new IllegalStateException("HTTP " + response.statusCode());
            }
            citiesByName = build(MAPPER.readTree(response.body()).path("response").path("docs"));
            LOGGER.info("[TourIndex] {} isim yüklendi", citiesByName.size());
        } catch (Exception e) {
            LOGGER.warn("[TourIndex] yenilenemedi, eldeki indeks kullanılıyor: {}", e.getMessage());
        } finally {
            loaded = true;
        }
    }

    static Map<String, Integer> build(JsonNode docs) {
        Map<String, Set<String>> cities = new HashMap<>();
        for (JsonNode doc : docs) {
            Set<String> set = cities.computeIfAbsent(key(doc.path("sname").asText("")), k -> new HashSet<>());
            doc.path("city").forEach(c -> set.add(c.asText()));
        }
        Map<String, Integer> out = new HashMap<>();
        cities.forEach((k, v) -> out.put(k, v.size()));
        out.remove("");
        return out;
    }

    /** "Hikayeden Adamlar - Mahalle" / "Redd (Akustik)" → isim kısmı; şehirden şehre değişen alt başlık atılır. */
    public static String key(String title) {
        String head = title == null ? "" : title.split("\\s+[-–]\\s+|\\s*:\\s+|\\s*\\(", 2)[0];
        return EventDuplicateKeys.normalizeTitle(head);
    }

    /** Testler için. */
    void load(Map<String, Integer> index) {
        citiesByName = index;
        loaded = true;
    }
}
