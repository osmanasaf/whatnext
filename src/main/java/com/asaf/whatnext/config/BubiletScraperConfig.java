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

    public static final String EVENT_CARD_SELECTOR = "a[href*='/etkinlik/']";
    public static final String EVENT_TITLE_SELECTOR = "h3";
    public static final String EVENT_LOCATION_SELECTOR = "p.text-xs.text-gray-500:has(svg)";
    public static final String EVENT_DATE_SELECTOR = "p.text-xs.text-gray-500:not(:has(svg))";
    public static final String EVENT_IMAGE_SELECTOR = "img";

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
