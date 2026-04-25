package com.asaf.whatnext.config;

import com.asaf.whatnext.enums.Biletix.BiletixCategory;
import com.asaf.whatnext.enums.Biletix.BiletixCity;
import com.asaf.whatnext.enums.Biletix.BiletixDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Configuration
public class BiletixScraperConfig {
    
    public static final String BASE_URL = "https://www.biletix.com";
    
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final DateTimeFormatter SEARCH_RESULT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy");

    public static final BiletixCategory DEFAULT_CATEGORY = BiletixCategory.MUSIC;
    public static final BiletixDate DEFAULT_DATE = BiletixDate.THISWEEK;
    public static final BiletixCity DEFAULT_CITY = BiletixCity.ISTANBUL;

    public static final String EVENT_CARD_SELECTOR = ".grid_21.alpha.omega.listevent.searchResultEvent";
    public static final String EVENT_TITLE_SELECTOR = ".ln1.searchResultEventName";
    public static final String EVENT_TITLE_MOBILE_SELECTOR = ".searchResultEventNameMobile";
    public static final String EVENT_LINK_SELECTOR = "a[href*='/etkinlik/']";
    public static final String EVENT_DATE_SELECTOR = ".fld3 .ln1";
    public static final String EVENT_SECONDARY_DATE_SELECTOR = ".fld3 .ln2";
    public static final String EVENT_VENUE_SELECTOR = ".searchResultPlace";
    public static final String EVENT_LOCATION_SELECTOR = ".searchResultCity";
    /**
     * Kartta mekan: sırayla dene. Tarih alanındaki .ln2 ile karışmaması için
     * genel ".searchResult .ln2" kullanılmaz (tarih {@link #EVENT_SECONDARY_DATE_SELECTOR}).
     */
    public static final String[] EVENT_VENUE_ALTERNATE_SELECTORS = {
            EVENT_VENUE_SELECTOR,
            ".searchResultEventPlace",
            "div.searchResultEvent .searchResultPlace",
            "a[href*='/mekan/']"
    };
    public static final String LOAD_MORE_BUTTON_SELECTOR = ".search_load_more";

    private final ScraperConfig scraperConfig;
    
    @Value("${biletix.load.more.attempts:15}")
    private int maxLoadMoreAttempts;
    
    @Value("${biletix.load.more.wait:1500}")
    private int loadMoreWaitMs;

    @Autowired
    public BiletixScraperConfig(ScraperConfig scraperConfig) {
        this.scraperConfig = scraperConfig;
    }

    public String getChromePath() {
        return scraperConfig.getChromePath();
    }

    public int getWaitTimeoutSeconds() {
        return scraperConfig.getWaitTimeoutSeconds();
    }

    public int getMaxLoadMoreAttempts() {
        return maxLoadMoreAttempts;
    }

    public int getLoadMoreWaitMs() {
        return loadMoreWaitMs;
    }

    public String buildSearchUrl(BiletixCategory category, BiletixCity city, LocalDate startDate, LocalDate endDate) {
        String dateParam;
        String hashParam = "";
        if (startDate != null && endDate != null) {
            dateParam = String.format("%s-%s",
                    startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            hashParam = String.format(",subcat_interval:%sTO%s",
                    startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        } else {
            dateParam = DEFAULT_DATE.getValue();
        }
        return String.format(
                "%s/search/TURKIYE/tr?category_sb=%s&date_sb=%s&city_sb=%s#!category_sb:%s,city_sb:%s%s",
                BASE_URL,
                category.getValue(),
                dateParam,
                city.getValue(),
                category.getValue(),
                city.getValue(),
                hashParam
        );
    }
}