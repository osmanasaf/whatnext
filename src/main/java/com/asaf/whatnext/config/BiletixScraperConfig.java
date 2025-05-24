package com.asaf.whatnext.config;

import com.asaf.whatnext.enums.Biletix.BiletixCategory;
import com.asaf.whatnext.enums.Biletix.BiletixCity;
import com.asaf.whatnext.enums.Biletix.BiletixDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

@Configuration
public class BiletixScraperConfig {
    // URL and browser configuration
    public static final String BASE_URL = "https://www.biletix.com";
    
    @Value("${biletix.chrome.path:C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe}")
    private String chromePath;
    
    @Value("${biletix.wait.timeout:60}")
    private int waitTimeoutSeconds;
    
    @Value("${biletix.load.more.attempts:20}")
    private int maxLoadMoreAttempts;
    
    @Value("${biletix.load.more.wait:2000}")
    private int loadMoreWaitMs;

    // Date formatters
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final DateTimeFormatter SEARCH_RESULT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy");

    // Default search parameters
    public static final BiletixCategory DEFAULT_CATEGORY = BiletixCategory.MUSIC;
    public static final BiletixDate DEFAULT_DATE = BiletixDate.THISWEEK;
    public static final BiletixCity DEFAULT_CITY = BiletixCity.ISTANBUL;

    // CSS Selectors
    public static final String EVENT_CARD_SELECTOR = ".grid_21.alpha.omega.listevent.searchResultEvent";
    public static final String EVENT_TITLE_SELECTOR = ".ln1.searchResultEventName";
    public static final String EVENT_TITLE_MOBILE_SELECTOR = ".searchResultEventNameMobile";
    public static final String EVENT_LINK_SELECTOR = "a[href*='/etkinlik/']";
    public static final String EVENT_DATE_SELECTOR = ".fld3 .ln1";
    public static final String EVENT_SECONDARY_DATE_SELECTOR = ".fld3 .ln2";
    public static final String EVENT_VENUE_SELECTOR = ".searchResultPlace";
    public static final String EVENT_LOCATION_SELECTOR = ".searchResultCity";
    public static final String LOAD_MORE_BUTTON_SELECTOR = ".search_load_more";

    public String getChromePath() {
        return chromePath;
    }

    public int getWaitTimeoutSeconds() {
        return waitTimeoutSeconds;
    }

    public int getMaxLoadMoreAttempts() {
        return maxLoadMoreAttempts;
    }

    public int getLoadMoreWaitMs() {
        return loadMoreWaitMs;
    }

    public String buildSearchUrl(BiletixCategory category) {
        return String.format(
                "%s/search/TURKIYE/tr?category_sb=%s&date_sb=%s&city_sb=%s#!category_sb:%s,city_sb:%s,date_sb:%s",
                BASE_URL,
                category.getValue(),
                DEFAULT_DATE.getValue(),
                DEFAULT_CITY.getValue(),
                category.getValue(),
                DEFAULT_CITY.getValue(),
                DEFAULT_DATE.getValue()
        );
    }
}