package com.asaf.whatnext.scraper;

import com.asaf.whatnext.config.BiletinoScraperConfig;
import com.asaf.whatnext.enums.Biletino.BiletinoCategory;
import com.asaf.whatnext.enums.Biletino.BiletinoCity;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.scraper.biletino.BiletinoCardParser;
import com.asaf.whatnext.scraper.biletino.BiletinoHttp;
import com.asaf.whatnext.scraper.biletino.BiletinoVenueEnricher;
import com.asaf.whatnext.services.ScrapeHealthService;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.asaf.whatnext.config.BiletinoScraperConfig.*;

/**
 * Biletino arama sonucu sunucuda render ediliyor, bu yüzden tarayıcı yerine düz HTTP GET + Jsoup
 * kullanılır. {@code categoryid}/{@code city} parametreleri filtreyi doğru uyguluyor (2026-09-09
 * canlı doğrulama), ama şehir yine de kart üzerindeki değerle denetlenir: filtre sessizce bozulursa
 * etkinlikler yanlış şehirle kaydedilmek yerine {@code unknownCity} olarak sağlık özetine düşer.
 */
@Service
@Order(30)
public class BiletinoScraper extends BaseScraper {

    private final BiletinoScraperConfig config;
    private final BiletinoCardParser cardParser = new BiletinoCardParser();
    private final ScrapeHealthService scrapeHealthService;
    private final double minParseRatio;
    private final boolean enrichVenueFromDetail;

    @Autowired
    public BiletinoScraper(
            BiletinoScraperConfig config,
            ScrapeHealthService scrapeHealthService,
            @Value("${whatnext.scrape-health.min-parse-ratio:0.8}") double minParseRatio,
            @Value("${biletino.enrich-venue-from-detail:true}") boolean enrichVenueFromDetail) {
        super(null, 0, BiletinoScraper.class);
        this.config = config;
        this.scrapeHealthService = scrapeHealthService;
        this.minParseRatio = minParseRatio;
        this.enrichVenueFromDetail = enrichVenueFromDetail;
    }

    @Override
    public List<ScrapedEventData> scrapeEvents(String city) {
        logger.info("Scraping events from Biletino for city: " + city);

        BiletinoCity selectedCity = parseCity(city);
        ScrapeRunStats stats = new ScrapeRunStats(getSourceName());

        List<ScrapedEventData> allData = new ArrayList<>();
        for (BiletinoCategory category : Arrays.asList(
                BiletinoCategory.MUSIC, BiletinoCategory.THEATRE, BiletinoCategory.COMEDY)) {
            allData.addAll(scrapeCategory(category, selectedCity, stats));
        }

        logger.info("Scraped " + allData.size() + " events from Biletino");
        long validCount = allData.stream().filter(ScrapedEventData::isValid).count();
        logger.info("Valid events: " + validCount + "/" + allData.size());

        finishAndRecordStats(stats, scrapeHealthService, minParseRatio);

        return allData;
    }

    private List<ScrapedEventData> scrapeCategory(BiletinoCategory category, BiletinoCity city, ScrapeRunStats stats) {
        List<ScrapedEventData> events = new ArrayList<>();

        List<Element> cards;
        try {
            cards = BiletinoHttp.get(config.buildSearchUrl(category, city), config.getTimeoutMs())
                    .select(EVENT_CARD_SELECTOR);
        } catch (Exception e) {
            logger.error("Biletino arama isteği başarısız: " + category.name(), e);
            return events;
        }

        for (Element card : cards) {
            stats.recordCardSeen();
            try {
                ScrapedEventData data = cardParser.parseCard(card, category, city);
                if (data == null || !data.isValid()) {
                    stats.recordParseFailed(safeSnippet(card));
                    continue;
                }
                // Şehir kartın kendi "İlçe, Şehir" alanından gelir; site filtresi bozulursa
                // etkinlik yanlış şehirle kaydedilmesin diye burada elenir.
                if (!city.getValue().equals(data.getCity())) {
                    stats.recordUnknownCity();
                    continue;
                }
                enrichVenueIfNeeded(data);
                events.add(data);
                stats.recordParsed(data.getTitle());
                if (data.getVenueName() == null || data.getVenueName().isBlank()) {
                    stats.recordMissingVenue(data.getTitle());
                }
                if (data.getStartDate() == null) {
                    stats.recordMissingDate(data.getTitle());
                }
            } catch (Exception e) {
                stats.recordParseFailed(safeSnippet(card));
                logger.debug("Error extracting card", e);
            }
        }

        return events;
    }

    /**
     * Liste kartındaki mekân alanı ilçe adıdır (canlı denetimde doğrulandı, ör. "Kadıköy"); gerçek
     * mekân adı detay sayfası title etiketinden düz HTTP GET ile alınır. Başarısızlıkta mevcut değer korunur.
     */
    private void enrichVenueIfNeeded(ScrapedEventData data) {
        if (!enrichVenueFromDetail) {
            return;
        }
        try {
            String realVenue = BiletinoVenueEnricher.fetchVenueName(data.getTicketUrl());
            if (realVenue != null && !realVenue.isBlank()) {
                data.setVenueName(realVenue);
            }
        } catch (Exception e) {
            logger.debug("Biletino mekân zenginleştirme hatası: " + data.getTicketUrl(), e);
        }
    }

    private static String safeSnippet(Element card) {
        String html = card.outerHtml();
        return html.substring(0, Math.min(120, html.length()));
    }

    private BiletinoCity parseCity(String city) {
        return Arrays.stream(BiletinoCity.values())
                .filter(c -> c.getValue().equalsIgnoreCase(city))
                .findFirst()
                .orElse(BiletinoCity.ISTANBUL);
    }

    @Override
    public String getSourceName() {
        return "Biletino";
    }

    @Override
    public String getSourceUrl() {
        return BASE_URL;
    }

    @Override
    public EventSourceType getEventSourceType() {
        return EventSourceType.BILETINO;
    }

    @Override
    protected String getDefaultCity() {
        return BiletinoCity.ISTANBUL.getValue();
    }
}
