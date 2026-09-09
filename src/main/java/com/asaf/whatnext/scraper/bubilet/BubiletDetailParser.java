package com.asaf.whatnext.scraper.bubilet;

import com.asaf.whatnext.config.BubiletScraperConfig;
import com.asaf.whatnext.scraper.common.TurkishDateParser;
import com.asaf.whatnext.scraper.web.HtmlFetcher;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bubilet etkinlik detay (çoklu tarih, tek günlü mekân) okuma. Detay sayfası sunucuda render
 * ediliyor, bu yüzden tarayıcı yerine {@link HtmlFetcher} kullanılır (2026-09-09 canlı doğrulama:
 * {@code #biletler-section} ve mekân bağlantısı düz GET yanıtında mevcut).
 */
public class BubiletDetailParser {

    private static final Logger LOG = LoggerFactory.getLogger(BubiletDetailParser.class);

    private final int biletlerSectionRolloverDays;
    private final int timeoutMs;

    public BubiletDetailParser(int biletlerSectionRolloverDays, int timeoutMs) {
        this.biletlerSectionRolloverDays = biletlerSectionRolloverDays;
        this.timeoutMs = timeoutMs;
    }

    public record MultiDateDetail(List<LocalDate> dates, String venue) {
    }

    public MultiDateDetail loadMultiDate(String url) {
        Document doc = HtmlFetcher.getOrNull(url, timeoutMs);
        if (doc == null) {
            LOG.warn("Bubilet detay sayfası okunamadı: {}", url);
            return new MultiDateDetail(List.of(), null);
        }
        Element section = doc.getElementById(BubiletScraperConfig.DETAIL_BILETLER_SECTION_ID);
        if (section == null) {
            return new MultiDateDetail(List.of(), readVenue(doc));
        }
        List<LocalDate> dates = TurkishDateParser.extractDayMonthFromTurkishText(
                section.text(), com.asaf.whatnext.utils.TrClock.today(), biletlerSectionRolloverDays);
        return new MultiDateDetail(dates, readVenue(doc));
    }

    public String loadVenueForSingleDate(String url) {
        Document doc = HtmlFetcher.getOrNull(url, timeoutMs);
        return doc == null ? null : readVenue(doc);
    }

    /**
     * Sırayla dener; hiçbiri dolu değilse {@code null}. {@code selectFirst} şart:
     * {@code select(...).text()} çok mekânlı etkinliklerde tüm mekân adlarını tek dizede
     * birleştiriyor ("A Sahnesi B Sahnesi C Sahnesi"), Selenium'daki {@code findElement} ise
     * ilk eşleşmeyi veriyordu.
     */
    static String readVenue(Document doc) {
        for (String selector : BubiletScraperConfig.DETAIL_VENUE_LINK_SELECTORS) {
            Element el = doc.selectFirst(selector);
            if (el != null && !el.text().trim().isEmpty()) {
                return el.text().trim();
            }
        }
        return null;
    }
}
