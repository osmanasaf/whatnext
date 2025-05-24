package com.asaf.whatnext.config;

import com.asaf.whatnext.enums.Biletino.BiletinoCategory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

@Configuration
public class BiletinoScraperConfig {
    // URL and browser configuration
    public static final String BASE_URL = "https://biletino.com";
    
    @Value("${biletino.chrome.path:C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe}")
    private String chromePath;
    
    @Value("${biletino.wait.timeout:60}")
    private int waitTimeoutSeconds;
    
    @Value("${biletino.scroll.attempts:20}")
    private int maxScrollAttempts;
    
    @Value("${biletino.scroll.wait:2000}")
    private int scrollWaitMs;

    // Date formatters
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final DateTimeFormatter SEARCH_RESULT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    // Default search parameters
    public static final BiletinoCategory DEFAULT_CATEGORY = BiletinoCategory.MUSIC;
    public static final String DEFAULT_LOCATION = "İstanbul";
    public static final String DEFAULT_DATE_PARAM = "next10days";

    // CSS Selectors
    public static final String EVENT_CARD_SELECTOR = "div.col-md-4.product";
    public static final String EVENT_TITLE_SELECTOR = "h3.title";
    public static final String EVENT_DATE_SELECTOR = "p.card-text.date";
    public static final String EVENT_LOCATION_SELECTOR = "p.card-text.location";
    public static final String EVENT_LINK_SELECTOR = "a.event-url";
    public static final String EVENT_IMAGE_SELECTOR = "img";
    public static final String SEARCH_LOAD_END_SELECTOR = "p#search-load-end";

    public String getChromePath() {
        return chromePath;
    }

    public int getWaitTimeoutSeconds() {
        return waitTimeoutSeconds;
    }

    public int getMaxScrollAttempts() {
        return maxScrollAttempts;
    }

    public int getScrollWaitMs() {
        return scrollWaitMs;
    }

    public String buildSearchUrl(BiletinoCategory category) {
        return String.format(
                "%s/en/search/?start=0&count=0&ajax=false&loadmore=false&autochange=true&userid=&query=&performer=&category=%s&location=%s&date=%s",
                BASE_URL,
                category.getValue(),
                DEFAULT_LOCATION,
                DEFAULT_DATE_PARAM
        );
    }
}