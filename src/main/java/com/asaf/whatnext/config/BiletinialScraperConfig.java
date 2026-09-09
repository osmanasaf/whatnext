package com.asaf.whatnext.config;

import com.asaf.whatnext.enums.Biletinial.BiletinialCategory;
import com.asaf.whatnext.enums.Biletinial.BiletinialCity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

@Configuration
public class BiletinialScraperConfig {
    
    public static final String BASE_URL = "https://biletinial.com";

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public static final BiletinialCategory DEFAULT_CATEGORY = BiletinialCategory.MUSIC;
    public static final BiletinialCity DEFAULT_CITY = BiletinialCity.ISTANBUL;

    public static final String EVENT_CARD_SELECTOR = "#kategori__etkinlikler li";
    public static final String EVENT_TITLE_SELECTOR = "h3 a";
    public static final String EVENT_DATE_SELECTOR = "span";
    public static final String EVENT_LOCATION_SELECTOR = "address";
    public static final String EVENT_LINK_SELECTOR = "a";
    public static final String EVENT_IMAGE_SELECTOR = "img[src*='merlincdn.net']";

    /** Liste ve detay sayfaları sunucuda render ediliyor; tarayıcı yerine düz GET (bkz. HtmlFetcher). */
    @Value("${biletinial.http.timeout.ms:20000}")
    private int timeoutMs;

    public int getTimeoutMs() {
        return timeoutMs;
    }
} 