package com.asaf.whatnext.scraper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bir scrape koşusunun sağlık istatistikleri: kart/parse oranları + kova başına örnek başlıklar.
 * Site DOM'u değişince tek bakışta ("parsed/cardsSeen düştü") fark edilsin diye.
 */
public class ScrapeRunStats {

    private static final int MAX_SAMPLES_PER_BUCKET = 3;

    private final String sourceName;
    private final long startMs = System.currentTimeMillis();
    private long endMs = -1;

    private int cardsSeen;
    private int parsed;
    private int parseFailed;
    private int missingVenue;
    private int missingDate;
    private int multiVenue;
    private int priceExtracted;
    private int unknownCity;

    private final Map<String, List<String>> samples = new LinkedHashMap<>();

    public ScrapeRunStats(String sourceName) {
        this.sourceName = sourceName;
    }

    public void recordCardSeen() {
        cardsSeen++;
    }

    public void recordParsed(String title) {
        parsed++;
        sample("parsed", title);
    }

    public void recordParseFailed(String rawSnippet) {
        parseFailed++;
        sample("parseFailed", rawSnippet);
    }

    public void recordMissingVenue(String title) {
        missingVenue++;
        sample("missingVenue", title);
    }

    public void recordMissingDate(String title) {
        missingDate++;
        sample("missingDate", title);
    }

    public void recordMultiVenue(String title) {
        multiVenue++;
        sample("multiVenue", title);
    }

    public void recordPriceExtracted() {
        priceExtracted++;
    }

    public void recordUnknownCity() {
        unknownCity++;
    }

    public void finish() {
        this.endMs = System.currentTimeMillis();
    }

    private void sample(String bucket, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        List<String> list = samples.computeIfAbsent(bucket, k -> new ArrayList<>());
        if (list.size() < MAX_SAMPLES_PER_BUCKET) {
            list.add(value);
        }
    }

    public double parseRatio() {
        return cardsSeen == 0 ? 1.0 : (double) parsed / cardsSeen;
    }

    public boolean isHealthy(double minParseRatio) {
        return cardsSeen == 0 || parseRatio() >= minParseRatio;
    }

    public long durationMs() {
        return (endMs < 0 ? System.currentTimeMillis() : endMs) - startMs;
    }

    public String getSourceName() {
        return sourceName;
    }

    public int getCardsSeen() {
        return cardsSeen;
    }

    public int getParsed() {
        return parsed;
    }

    public int getParseFailed() {
        return parseFailed;
    }

    public int getMissingVenue() {
        return missingVenue;
    }

    public int getMissingDate() {
        return missingDate;
    }

    public int getMultiVenue() {
        return multiVenue;
    }

    public int getPriceExtracted() {
        return priceExtracted;
    }

    public int getUnknownCity() {
        return unknownCity;
    }

    public Map<String, List<String>> getSamples() {
        return samples;
    }

    public String toSummaryLine() {
        return String.format(
                "%s: cardsSeen=%d parsed=%d (%.0f%%) parseFailed=%d missingVenue=%d missingDate=%d multiVenue=%d "
                        + "priceExtracted=%d unknownCity=%d süre=%dms",
                sourceName, cardsSeen, parsed, parseRatio() * 100, parseFailed, missingVenue, missingDate,
                multiVenue, priceExtracted, unknownCity, durationMs());
    }
}
