package com.asaf.whatnext.config;

import com.asaf.whatnext.enums.Biletino.BiletinoCategory;
import com.asaf.whatnext.enums.Biletino.BiletinoCity;
import org.springframework.beans.factory.annotation.Autowired;
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
    public static final String SEARCH_LOAD_END_SELECTOR = "p#search-load-end";

    private final ScraperConfig scraperConfig;
    
    @Value("${biletino.scroll.attempts:15}")
    private int maxScrollAttempts;
    
    @Value("${biletino.scroll.wait:1500}")
    private int scrollWaitMs;

    @Autowired
    public BiletinoScraperConfig(ScraperConfig scraperConfig) {
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

    public String buildSearchUrl(BiletinoCategory category) {
        return buildSearchUrl(category, BiletinoCity.ISTANBUL);
    }

    /**
     * {@code category}/{@code location} yerine {@code categoryid}/{@code city} kullanılır — canlı
     * doğrulamada (2026-07-13) eski isimlerin siteye hiçbir etkisi olmadığı, gerçek form alanlarının
     * bunlar olduğu tespit edildi (aksi halde kategori/şehir filtresiz aynı sonuç listesi döner).
     */
    public String buildSearchUrl(BiletinoCategory category, BiletinoCity city) {
        return String.format(
                "%s/en/search/?start=0&count=0&ajax=false&loadmore=false&autochange=true&userid=&query=&performer=&categoryid=%s&city=%s&date=%s",
                BASE_URL,
                category.getValue(),
                city.getValue(),
                DEFAULT_DATE_PARAM
        );
    }
}