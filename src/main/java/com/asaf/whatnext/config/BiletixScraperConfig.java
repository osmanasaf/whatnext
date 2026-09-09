package com.asaf.whatnext.config;

import com.asaf.whatnext.enums.Biletix.BiletixCategory;
import com.asaf.whatnext.enums.Biletix.BiletixCity;
import com.asaf.whatnext.enums.Biletix.BiletixDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
public class BiletixScraperConfig {

    public static final String BASE_URL = "https://www.biletix.com";

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public static final BiletixCategory DEFAULT_CATEGORY = BiletixCategory.MUSIC;
    public static final BiletixDate DEFAULT_DATE = BiletixDate.THISWEEK;
    public static final BiletixCity DEFAULT_CITY = BiletixCity.ISTANBUL;

    /**
     * 2026-09 Angular arayüzü sonuçları bu Solr uç noktasından çeker; DOM taraması yerine doğrudan
     * buraya sorulur (canlı doğrulama 2026-09-09: çerez/başlık gerekmiyor, anonim POST yeterli).
     * Kritik fark: sitenin kendi isteğinde region filtresi YOK — URL'de ISTANBUL yazsa da Antalya,
     * Denizli, Ankara kartları geliyor. Biz {@code fq=region:...} ile daraltıp, belgedeki
     * {@code city} alanıyla şehri kesinleştiriyoruz (region = pazarlama bölgesi, tek şehir değil).
     */
    public static final String SOLR_URL = BASE_URL + "/solr/tr/select";

    /** {@code image_url} alanı yalnız dosya adı taşır; kartlardaki tam adres bu önekle kuruluyor. */
    public static final String IMAGE_URL_PREFIX = BASE_URL + "/static/images/live/event/eventimages/";

    /** Etkinlik sayfası: {@code /etkinlik/{id}/{REGION}/tr} (kartlardaki href biçimi). */
    public static final String EVENT_URL_FORMAT = BASE_URL + "/etkinlik/%s/%s/tr";

    /** Kapsam eskisiyle aynı tutuldu; {@code sinema$ART} bilinçli olarak dışarıda. */
    public static final String[] MUSIC_SUBCATEGORIES = {"pop", "rock", "jazz", "klasik", "alternatif"};
    public static final String[] ART_SUBCATEGORIES = {"tiyatro", "gosteri", "bale_dans", "stand_up"};

    @Value("${biletix.solr.rows:2000}")
    private int solrRows;

    @Value("${biletix.solr.timeout.seconds:30}")
    private int timeoutSeconds;

    public int getSolrRows() {
        return solrRows;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /** {@code subcategory:("pop$MUSIC" OR "rock$MUSIC" ...)} */
    public static String buildSubcategoryFilter(BiletixCategory category) {
        String[] subcategories = category == BiletixCategory.MUSIC ? MUSIC_SUBCATEGORIES : ART_SUBCATEGORIES;
        return "subcategory:(" + Stream.of(subcategories)
                .map(sub -> "\"" + sub + "$" + category.getValue() + "\"")
                .collect(Collectors.joining(" OR ")) + ")";
    }

    /** Solr {@code region} değeri şehrin büyük harfli hâli (ISTANBUL, ANKARA, IZMIR, ANTALYA). */
    public static String regionOf(BiletixCity city) {
        return city.getValue().toUpperCase(Locale.ROOT);
    }
}
