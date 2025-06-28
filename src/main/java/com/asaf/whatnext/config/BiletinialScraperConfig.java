package com.asaf.whatnext.config;

import com.asaf.whatnext.enums.Biletinial.BiletinialCategory;
import com.asaf.whatnext.enums.Biletinial.BiletinialCity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

@Configuration
public class BiletinialScraperConfig {
    // URL ve browser ayarları
    public static final String BASE_URL = "https://biletinial.com";

    @Value("${biletinial.chrome.path:C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe}")
    private String chromePath;

    @Value("${biletinial.wait.timeout:60}")
    private int waitTimeoutSeconds;

    @Value("${biletinial.scroll.attempts:20}")
    private int maxScrollAttempts;

    @Value("${biletinial.scroll.wait:2000}")
    private int scrollWaitMs;

    // Tarih formatları
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    // Varsayılan arama parametreleri
    public static final BiletinialCategory DEFAULT_CATEGORY = BiletinialCategory.MUSIC;
    public static final BiletinialCity DEFAULT_CITY = BiletinialCity.ISTANBUL;

    // CSS Selectorlar
    public static final String EVENT_CARD_SELECTOR = "#kategori__etkinlikler li";
    public static final String EVENT_TITLE_SELECTOR = "h3 a";
    public static final String EVENT_DATE_SELECTOR = "span";
    public static final String EVENT_LOCATION_SELECTOR = "address";
    public static final String EVENT_LINK_SELECTOR = "a";
    public static final String EVENT_IMAGE_SELECTOR = "img[src*='merlincdn.net']";

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
} 