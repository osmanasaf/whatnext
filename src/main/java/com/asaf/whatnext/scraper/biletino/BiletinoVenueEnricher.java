package com.asaf.whatnext.scraper.biletino;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Liste kartındaki mekân alanı aslında ilçe adıdır (ör. "Kadıköy, İstanbul" → "Kadıköy"); canlı
 * denetimde doğrulandı. Gerçek mekân adı yalnızca detay sayfasının {@code <title>} etiketinde var:
 * {@code "{başlık} | {tarih} | {mekân} | Biletino"}. Selenium navigasyonu yerine düz HTTP GET +
 * Jsoup yeterli (title sunucu tarafında render ediliyor).
 */
public final class BiletinoVenueEnricher {

    private static final Logger LOG = LoggerFactory.getLogger(BiletinoVenueEnricher.class);
    private static final Pattern TITLE_PATTERN = Pattern.compile("^.*\\|.*\\|\\s*(.+?)\\s*\\|\\s*Biletino\\s*$");
    private static final int TIMEOUT_MS = 5000;

    private BiletinoVenueEnricher() {
    }

    /** Başarısız olursa (ağ hatası, zaman aşımı, beklenmeyen title) {@code null} döner — çağıran mevcut değeri korur. */
    public static String fetchVenueName(String ticketUrl) {
        if (ticketUrl == null || ticketUrl.isBlank()) {
            return null;
        }
        try {
            String title = Jsoup.connect(ticketUrl)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0")
                    .get()
                    .title();
            return extractVenueFromTitle(title);
        } catch (Exception e) {
            LOG.debug("Biletino mekân zenginleştirme başarısız: {}", ticketUrl, e);
            return null;
        }
    }

    static String extractVenueFromTitle(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }
        Matcher m = TITLE_PATTERN.matcher(title.trim());
        if (m.matches()) {
            String v = m.group(1).trim();
            return v.isEmpty() ? null : v;
        }
        return null;
    }
}
