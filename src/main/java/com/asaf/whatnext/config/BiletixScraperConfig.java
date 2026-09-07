package com.asaf.whatnext.config;

import com.asaf.whatnext.enums.Biletix.BiletixCategory;
import com.asaf.whatnext.enums.Biletix.BiletixCity;
import com.asaf.whatnext.enums.Biletix.BiletixDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    /**
     * Canlı doğrulamada (2026-07-13) tespit edildi: {@code /search/TURKIYE/tr?city_sb=...} şehri
     * filtrelemiyor (site "Tüm Türkiye" gösteriyor); {@code /search/{ŞEHİR}/tr?category_sb=...} da
     * kategoriyi filtrelemiyor. Şehir VE kategori ikisinin birlikte doğru çalıştığı tek kombinasyon:
     * {@code /search/{ŞEHİR}/tr#subcategory:{alt}${KATEGORİ}&qt=standard} — alt-kategori zorunlu,
     * bu yüzden kategori başına birden fazla URL (alt-kategoriler) gezilip birleştirilir.
     */
    public static final String[] MUSIC_SUBCATEGORIES = {"pop", "rock", "jazz", "klasik", "alternatif"};
    public static final String[] ART_SUBCATEGORIES = {"tiyatro", "gosteri", "bale_dans", "stand_up"};

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

    /**
     * Kategori için şehir+kategori'nin ikisinin de doğru filtrelendiği alt-kategori URL'lerini
     * döndürür (bkz. {@link #MUSIC_SUBCATEGORIES}/{@link #ART_SUBCATEGORIES}). Çağıran taraf her
     * URL'yi ayrı ayrı tarayıp sonuçları (ticketUrl'e göre) tekilleştirmeli — aynı etkinlik birden
     * fazla alt-kategoride görünebilir.
     */
    public List<String> buildSubcategoryUrls(BiletixCategory category, BiletixCity city) {
        String[] subcategories = category == BiletixCategory.MUSIC ? MUSIC_SUBCATEGORIES : ART_SUBCATEGORIES;
        String cityPath = city.getValue().toUpperCase(Locale.ROOT);
        List<String> urls = new ArrayList<>();
        for (String sub : subcategories) {
            urls.add(String.format("%s/search/%s/tr#subcategory:%s$%s&qt=standard",
                    BASE_URL, cityPath, sub, category.getValue()));
        }
        return urls;
    }
}