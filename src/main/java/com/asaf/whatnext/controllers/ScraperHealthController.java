package com.asaf.whatnext.controllers;

import com.asaf.whatnext.scraper.ScrapeRunStats;
import com.asaf.whatnext.services.ScrapeHealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tüm kaynakların son scrape koşusu sağlığı — site DOM'u değişince ({@code parsed/cardsSeen} düşerse)
 * tek istekle anlaşılır. Kaynak başına ayrı satır ({@code Map<sourceName, ...>}).
 */
@RestController
@RequestMapping("/api/scraper")
public class ScraperHealthController {

    private final ScrapeHealthService scrapeHealthService;
    private final double minParseRatio;

    @Autowired
    public ScraperHealthController(
            ScrapeHealthService scrapeHealthService,
            @Value("${whatnext.scrape-health.min-parse-ratio:0.8}") double minParseRatio) {
        this.scrapeHealthService = scrapeHealthService;
        this.minParseRatio = minParseRatio;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        for (Map.Entry<String, ScrapeRunStats> e : scrapeHealthService.getAllLastRuns().entrySet()) {
            body.put(e.getKey(), toJson(e.getValue()));
        }
        return ResponseEntity.ok(body);
    }

    private Map<String, Object> toJson(ScrapeRunStats s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("cardsSeen", s.getCardsSeen());
        m.put("parsed", s.getParsed());
        m.put("parseRatio", s.parseRatio());
        m.put("parseFailed", s.getParseFailed());
        m.put("missingVenue", s.getMissingVenue());
        m.put("missingDate", s.getMissingDate());
        m.put("multiVenue", s.getMultiVenue());
        m.put("priceExtracted", s.getPriceExtracted());
        m.put("unknownCity", s.getUnknownCity());
        m.put("durationMs", s.durationMs());
        m.put("healthy", s.isHealthy(minParseRatio));
        Map<String, List<String>> samples = new LinkedHashMap<>(s.getSamples());
        m.put("samples", samples);
        return m;
    }
}
