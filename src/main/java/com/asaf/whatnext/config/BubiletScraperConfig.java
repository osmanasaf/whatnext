package com.asaf.whatnext.config;

import com.asaf.whatnext.enums.Bubilet.BubiletCategory;
import com.asaf.whatnext.enums.Bubilet.BubiletCity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BubiletScraperConfig {
    
    public static final String BASE_URL = "https://www.bubilet.com.tr";

    public static final BubiletCategory DEFAULT_CATEGORY = BubiletCategory.THEATRE;
    public static final BubiletCity DEFAULT_CITY = BubiletCity.ISTANBUL;

    /**
     * 2026 ızgara: kart kapsayıcısı — görsel linki {@code a.group.block[href*='/etkinlik/']} +
     * metin bloğu {@code div.min-h-0.min-w-0} (başlık/mekân/tarih/fiyat) aynı üst div'de.
     * Canlıda doğrulandı (bubilet.com.tr, 2026-07-11): 48/48 kart eşleşti.
     */
    public static final String EVENT_CARD_SELECTOR =
            "div:has(> a.group.block[href*='/etkinlik/']):has(div.min-h-0.min-w-0 h3)";
    /** Kart içindeki başlık {@code <a href="/.../etkinlik/...">} — h3 metni başlıktır. */
    public static final String EVENT_TITLE_ANCHOR = "a[href*='/etkinlik/']";
    public static final String EVENT_TITLE_SELECTOR = "h3";
    /** Mekân adı + kanonik mekân slug'ı (href'ten) — metin sezgisi gerekmez. */
    public static final String EVENT_VENUE_ANCHOR = "a[href*='/mekan/']";
    /** Tarih satırı ({@code <p>}) + fiyat ({@code ₺}) bu anchor içinde. */
    public static final String EVENT_DATEPRICE_TRIGGER = "a.date-price-trigger";
    /** Fiyat rakamı span'ı, {@code date-price-trigger} içinde. */
    public static final String EVENT_PRICE_VALUE_SELECTOR = "span.tracking-tight";

    /**
     * Eski ızgara (2025 öncesi, ölü — sadece fallback): h3, sonra mekân {@code p}, sonra tarih {@code p}.
     */
    @Deprecated
    public static final String EVENT_VENUE_AFTER_H3 = "h3 + p";
    @Deprecated
    public static final String EVENT_DATE_AFTER_H3 = "h3 + p + p";
    @Deprecated
    public static final String EVENT_TEXT_BLOCK = ".min-h-0.min-w-0";
    @Deprecated
    public static final String EVENT_LOCATION_SELECTOR = "p.text-xs.text-gray-500:has(svg)";
    @Deprecated
    public static final String[] EVENT_VENUE_ALTERNATE_SELECTORS = {
            "h3 + p",
            "p.text-xs.text-gray-500:has(svg)",
            "p.text-xs:has(svg)"
    };
    @Deprecated
    public static final String EVENT_DATE_SELECTOR = "p.text-xs.text-gray-500:not(:has(svg))";
    public static final String EVENT_IMAGE_SELECTOR = "img";

    /** Etkinlik detay — bilet / seans blokları (çok tarihli parse) */
    public static final String DETAIL_BILETLER_SECTION_ID = "biletler-section";
    /** Detay sayfasında mekân adı (çok tarihli akışta liste boşsa) */
    public static final String[] DETAIL_VENUE_LINK_SELECTORS = {
            "#mekan-section a[href*='/mekan/'] h4",
            "a[href*='/mekan/'] h4",
            "a[href*='/mekan/']",
            "a[href*='/Mekan/']"
    };

    private final ScraperConfig scraperConfig;
    
    @Value("${bubilet.scroll.attempts:20}")
    private int maxScrollAttempts;
    
    @Value("${bubilet.scroll.wait:2000}")
    private int scrollWaitMs;

    @Autowired
    public BubiletScraperConfig(ScraperConfig scraperConfig) {
        this.scraperConfig = scraperConfig;
    }

    public String getChromePath() {
        return scraperConfig.getChromePath();
    }

    public int getWaitTimeoutSeconds() {
        return scraperConfig.getWaitTimeoutSeconds();
    }

    public int getMaxScrollAttempts() {
        return maxScrollAttempts;
    }

    public int getScrollWaitMs() {
        return scrollWaitMs;
    }

    public String buildSearchUrl(BubiletCategory category, BubiletCity city) {
        return String.format("%s/%s/etiket/%s", BASE_URL, city.getValue(), category.getValue());
    }
}
