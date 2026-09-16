package com.asaf.whatnext.services.scoring;

import com.asaf.whatnext.utils.EventDuplicateKeys;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * {@code popular_config.json}'daki elle seçilmiş popüler mekân ve sanatçı/oyun/komedyen listesi —
 * puanlama anında uygulanır.
 *
 * <p>Önceden bu liste yalnız uygulama açılışında DB'deki {@code Artist}/{@code Venue} kayıtlarına
 * yazılıyordu: sadece konserlerin {@code artist} ilişkisi puan alıyor (stand-up "Cem Yılmaz" 0),
 * açılıştan sonra kazınan yeni mekân/sanatçı ise bir sonraki yeniden başlatmaya kadar puansız kalıyordu.
 * Artık etkinlik başlığı (ve varsa sanatçı adı) üzerinden her türe uygulanır.
 */
@Component
public class PopularityCatalog {

    static final int CATALOG_SCORE = 10;

    /** Parantez içi tür ekleri ("Kuğu Gölü (Bale)") takma ad olarak kullanılmaz — her balede eşleşirdi. */
    private static final Set<String> GENERIC_WORDS = Set.of(
            "bale", "muzikal", "opera", "konser", "tiyatro", "stand up", "standup", "gosteri", "oyun");

    private final List<String> venueKeys = new ArrayList<>();
    private final List<String> multiWordPerformers = new ArrayList<>();
    private final List<String> singleWordPerformers = new ArrayList<>();

    public PopularityCatalog() {
        this(loadConfig());
    }

    PopularityCatalog(JsonNode config) {
        for (JsonNode venue : config.path("topVenues")) {
            String key = fold(venue.asText("")).replace(" ", "");
            if (!key.isEmpty()) {
                venueKeys.add(key);
            }
        }
        for (JsonNode performer : config.path("topArtists")) {
            for (String alias : aliases(performer.asText(""))) {
                (alias.indexOf(' ') >= 0 ? multiWordPerformers : singleWordPerformers).add(alias);
            }
        }
    }

    /** Mekân adında listedeki bir mekân geçiyorsa (boşluk/Türkçe harf farkı gözetmeden) 10. */
    public int venueScore(String venueName) {
        String key = fold(venueName).replace(" ", "");
        if (key.isEmpty()) {
            return 0;
        }
        for (String venue : venueKeys) {
            if (key.contains(venue)) {
                return CATALOG_SCORE;
            }
        }
        return 0;
    }

    /**
     * Metinlerden (başlık, sanatçı adı) biri listedeki bir isimle eşleşiyorsa 10. Çok kelimeli isim
     * kelime sınırında her yerde ("Konuşanlar - Hasan Can Kaya"), tek kelimelik isim yalnız metnin
     * başında eşleşir ("Duman Konseri" evet, "Doğan Güneş Tiyatrosu" hayır).
     * ponytail: ad listesi küçük olduğu için doğrusal tarama; liste binleri bulursa token dizinine geç.
     */
    public int performerScore(String... texts) {
        for (String text : texts) {
            String padded = " " + fold(text) + " ";
            if (padded.isBlank()) {
                continue;
            }
            for (String name : multiWordPerformers) {
                if (padded.contains(" " + name + " ")) {
                    return CATALOG_SCORE;
                }
            }
            for (String name : singleWordPerformers) {
                if (padded.startsWith(" " + name + " ")) {
                    return CATALOG_SCORE;
                }
            }
        }
        return 0;
    }

    /** "Kel Diva (Haluk Bilginer & Zuhal Olcay)" → [kel diva, haluk bilginer, zuhal olcay]. */
    static List<String> aliases(String entry) {
        List<String> out = new ArrayList<>();
        for (String part : entry.split("[()&]")) {
            String alias = fold(part);
            if (!alias.isEmpty() && !GENERIC_WORDS.contains(alias)) {
                out.add(alias);
            }
        }
        return out;
    }

    /** Küçük harf + noktalama → boşluk + Türkçe harfler ASCII'ye (kaynaklar arası "Sener Sen" yazımı da eşleşsin). */
    public static String fold(String s) {
        return EventDuplicateKeys.normalizeTitle(s)
                .replace('ı', 'i').replace('ğ', 'g').replace('ü', 'u')
                .replace('ş', 's').replace('ö', 'o').replace('ç', 'c');
    }

    private static JsonNode loadConfig() {
        try (InputStream in = new ClassPathResource("popular_config.json").getInputStream()) {
            return new ObjectMapper().readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException("popular_config.json okunamadı", e);
        }
    }
}
