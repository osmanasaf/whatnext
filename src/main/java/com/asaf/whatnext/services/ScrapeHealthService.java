package com.asaf.whatnext.services;

import com.asaf.whatnext.scraper.ScrapeRunStats;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Son scrape koşusu istatistikleri — {@code GET /api/scraper/health}. Site DOM'u değişince
 * tek istekle anlaşılsın diye (Postman koleksiyonuna eklendi).
 */
@Service
public class ScrapeHealthService {

    private final Map<String, ScrapeRunStats> lastRunBySource = new ConcurrentHashMap<>();

    public void recordRun(ScrapeRunStats stats) {
        if (stats == null) {
            return;
        }
        lastRunBySource.put(stats.getSourceName(), stats);
    }

    public Map<String, ScrapeRunStats> getAllLastRuns() {
        return lastRunBySource;
    }

    public ScrapeRunStats getLastRun(String source) {
        return lastRunBySource.get(source);
    }
}
