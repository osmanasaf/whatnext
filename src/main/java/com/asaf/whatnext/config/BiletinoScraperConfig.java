package com.asaf.whatnext.config;

import com.asaf.whatnext.enums.Biletino.BiletinoCategory;
import com.asaf.whatnext.enums.Biletino.BiletinoCity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

@Configuration
public class BiletinoScraperConfig {

    public static final String BASE_URL = "https://biletino.com";

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final DateTimeFormatter SEARCH_RESULT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public static final BiletinoCategory DEFAULT_CATEGORY = BiletinoCategory.MUSIC;
    public static final String DEFAULT_LOCATION = "İstanbul";
    public static final String DEFAULT_DATE_PARAM = "next10days";

    public static final String EVENT_CARD_SELECTOR = "div.col-md-4.product";
    public static final String EVENT_TITLE_SELECTOR = "h3.title";
    public static final String EVENT_DATE_SELECTOR = "p.card-text.date";
    public static final String EVENT_LOCATION_SELECTOR = "p.card-text.location";
    public static final String EVENT_LINK_SELECTOR = "a.event-url";
    public static final String EVENT_IMAGE_SELECTOR = "img";

    /** Arama sonucu sunucuda render ediliyor; tarayıcı gerekmez, düz GET yeterli. */
    public static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    /**
     * Sayfa boyutu. {@code count=0} sitenin varsayılan ilk sayfasını (9 kart) döndürür — canlı
     * doğrulamada (2026-09-09) İstanbul/müzik için 20 etkinliğin yalnız 9'u geliyordu. Tek istekte
     * tamamını almak için açıkça yüksek bir değer gönderilir; site mevcut kadarını döndürür.
     */
    @Value("${biletino.search.count:200}")
    private int searchCount;

    @Value("${biletino.search.timeout.ms:15000}")
    private int timeoutMs;

    public int getSearchCount() {
        return searchCount;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public String buildSearchUrl(BiletinoCategory category) {
        return buildSearchUrl(category, BiletinoCity.ISTANBUL);
    }

    /**
     * {@code category}/{@code location} yerine {@code categoryid}/{@code city} kullanılır — canlı
     * doğrulamada (2026-07-13) eski isimlerin siteye hiçbir etkisi olmadığı, gerçek form alanlarının
     * bunlar olduğu tespit edildi (aksi halde kategori/şehir filtresiz aynı sonuç listesi döner).
     * 2026-09-09'da yeniden doğrulandı: bu adlarla kategori ve şehir filtresi doğru çalışıyor.
     */
    public String buildSearchUrl(BiletinoCategory category, BiletinoCity city) {
        return String.format(
                "%s/en/search/?start=0&count=%d&ajax=false&loadmore=false&autochange=true&userid=&query=&performer=&categoryid=%s&city=%s&date=%s",
                BASE_URL,
                searchCount,
                category.getValue(),
                city.getValue(),
                DEFAULT_DATE_PARAM
        );
    }
}
