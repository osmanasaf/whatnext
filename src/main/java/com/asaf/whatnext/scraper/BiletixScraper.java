package com.asaf.whatnext.scraper;

import com.asaf.whatnext.config.BiletixScraperConfig;
import com.asaf.whatnext.enums.Biletix.BiletixCategory;
import com.asaf.whatnext.enums.Biletix.BiletixCity;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.scraper.biletix.BiletixSolrClient;
import com.asaf.whatnext.services.ScrapeHealthService;
import com.fasterxml.jackson.databind.JsonNode;
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

/**
 * Biletix: Selenium yerine sitenin kendi Solr uç noktası kullanılır
 * ({@link BiletixSolrClient}). Tarayıcı taramasının çözemediği sorun buydu — site sonuçları
 * URL'deki şehre göre filtrelemiyor ve kartta şehir alanı yok. Solr belgesinde {@code city}
 * bulunduğundan şehir artık kesin: {@code fq=region:...} kabaca daraltır (region bir pazarlama
 * bölgesi — ISTANBUL, Marmara'nın tamamını kapsar), belgedeki {@code city} ise şehri kesinleştirir.
 */
@Service
@Order(40)
public class BiletixScraper extends BaseScraper {

    private final BiletixSolrClient solrClient;
    private final ScrapeHealthService scrapeHealthService;
    private final double minParseRatio;

    @Autowired
    public BiletixScraper(
            BiletixScraperConfig config,
            ScrapeHealthService scrapeHealthService,
            @Value("${whatnext.scrape-health.min-parse-ratio:0.8}") double minParseRatio) {
        super(null, 0, BiletixScraper.class);
        this.solrClient = new BiletixSolrClient(config.getSolrRows(), config.getTimeoutSeconds());
        this.scrapeHealthService = scrapeHealthService;
        this.minParseRatio = minParseRatio;
    }

    @Override
    public List<ScrapedEventData> scrapeEvents(String city) {
        logger.info("Scraping events from Biletix for city: " + city);

        BiletixCity selectedCity = parseCity(city);
        ScrapeRunStats stats = new ScrapeRunStats(getSourceName());

        List<ScrapedEventData> allData = new ArrayList<>();
        for (BiletixCategory category : Arrays.asList(BiletixCategory.MUSIC, BiletixCategory.ART)) {
            allData.addAll(scrapeCategory(category, selectedCity, stats));
        }
        allData = dedupeByTicketUrl(allData);

        logger.info("Scraped " + allData.size() + " events from Biletix");
        long validCount = allData.stream().filter(ScrapedEventData::isValid).count();
        logger.info("Valid events: " + validCount + "/" + allData.size());

        finishAndRecordStats(stats, scrapeHealthService, minParseRatio);

        return allData;
    }

    private List<ScrapedEventData> scrapeCategory(BiletixCategory category, BiletixCity city, ScrapeRunStats stats) {
        List<ScrapedEventData> events = new ArrayList<>();
        List<JsonNode> docs;
        try {
            docs = solrClient.fetchDocs(category, city);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Biletix solr sorgusu kesildi: " + category, e);
            return events;
        } catch (Exception e) {
            logger.error("Biletix solr sorgusu başarısız: " + category, e);
            return events;
        }

        for (JsonNode doc : docs) {
            ScrapedEventData data = BiletixSolrClient.toEventData(doc, category);
            // Solr'daki region bir pazarlama bölgesi: region:ISTANBUL Marmara'nın tamamını
            // (Bursa, Kocaeli, Tekirdağ...) kapsıyor. Şehir, belgedeki city alanıyla süzülür.
            if (data != null && !city.getValue().equals(data.getCity())) {
                continue;
            }
            stats.recordCardSeen();
            if (data == null || !data.isValid()) {
                stats.recordParseFailed(doc.path("id").asText(""));
                continue;
            }
            events.add(data);
            stats.recordParsed(data.getTitle());
            if (data.getVenueName() == null || data.getVenueName().isBlank()) {
                stats.recordMissingVenue(data.getTitle());
            }
            if (data.getStartDate() == null) {
                stats.recordMissingDate(data.getTitle());
            }
            if (data.getCity() == null || data.getCity().isBlank()) {
                stats.recordUnknownCity();
            }
        }
        return events;
    }

    /** Aynı etkinlik birden fazla alt-kategoride görünebilir; ticketUrl'e göre ilk görüleni tutar. */
    private static List<ScrapedEventData> dedupeByTicketUrl(List<ScrapedEventData> items) {
        Map<String, ScrapedEventData> byUrl = new LinkedHashMap<>();
        for (ScrapedEventData d : items) {
            if (d == null || d.getTicketUrl() == null || d.getTicketUrl().isBlank()) {
                continue;
            }
            byUrl.putIfAbsent(d.getTicketUrl().trim().toLowerCase(Locale.ROOT), d);
        }
        return new ArrayList<>(byUrl.values());
    }

    private BiletixCity parseCity(String city) {
        return Arrays.stream(BiletixCity.values())
                .filter(c -> c.getValue().equalsIgnoreCase(city))
                .findFirst()
                .orElse(BiletixCity.ISTANBUL);
    }

    @Override
    public String getSourceName() {
        return "Biletix";
    }

    @Override
    public String getSourceUrl() {
        return BiletixScraperConfig.BASE_URL;
    }

    @Override
    public EventSourceType getEventSourceType() {
        return EventSourceType.BILETIX;
    }

    @Override
    protected String getDefaultCity() {
        return BiletixCity.ISTANBUL.getValue();
    }
}
