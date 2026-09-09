package com.asaf.whatnext.scraper;

import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.services.ScrapeHealthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Kaynaklar arası ortak iskelet: tarih süzme, sağlık istatistiği kaydı, varsayılan şehir.
 *
 * <p>2026-09-09'da tüm kaynaklar (Biletix, Biletino, Bubilet, Biletinial) Selenium'dan
 * çıkarıldı — hepsi ya sitenin JSON API'sini ya da sunucuda render edilen HTML'i düz HTTP ile
 * okuyor (bkz. {@code scraper.web.HtmlFetcher}). Tarayıcı yönetimi (driver ömrü, kaydırma,
 * çerez/overlay kapatma) bu yüzden buradan kaldırıldı.
 */
public abstract class BaseScraper implements EventSource {

    protected final Logger logger;

    protected BaseScraper(Class<?> loggerClass) {
        this.logger = LoggerFactory.getLogger(loggerClass);
    }

    @Override
    public List<ScrapedEventData> scrapeEvents() {
        return scrapeEvents(getDefaultCity());
    }

    @Override
    public abstract List<ScrapedEventData> scrapeEvents(String city);

    @Override
    public List<ScrapedEventData> scrapeEvents(String city, LocalDate startDate, LocalDate endDate) {
        List<ScrapedEventData> allData = scrapeEvents(city);
        return filterByDate(allData, startDate, endDate);
    }

    protected List<ScrapedEventData> filterByDate(List<ScrapedEventData> data, LocalDate startDate, LocalDate endDate) {
        List<ScrapedEventData> filtered = new ArrayList<>();
        for (ScrapedEventData item : data) {
            if (item.getStartDate() != null) {
                if ((item.getStartDate().isEqual(startDate) || item.getStartDate().isAfter(startDate)) &&
                    (item.getStartDate().isEqual(endDate) || item.getStartDate().isBefore(endDate))) {
                    filtered.add(item);
                }
            }
        }
        return filtered;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * Koşuyu bitirir, {@link ScrapeHealthService}'e kaydeder ve özet satırını loglar; eşik altında
     * WARN basar. Tüm kaynaklarda ({@code GET /api/scraper/health}) aynı davranış için ortak yol.
     */
    protected void finishAndRecordStats(ScrapeRunStats stats, ScrapeHealthService scrapeHealthService,
            double minParseRatio) {
        stats.finish();
        scrapeHealthService.recordRun(stats);
        logger.info(stats.toSummaryLine());
        if (!stats.isHealthy(minParseRatio)) {
            logger.warn(getSourceName() + " scrape sağlığı eşiğin altında (parseRatio<" + minParseRatio
                    + ") — site yapısı değişmiş olabilir. " + stats.toSummaryLine());
        }
    }

    public abstract EventSourceType getEventSourceType();

    protected abstract String getDefaultCity();
}
