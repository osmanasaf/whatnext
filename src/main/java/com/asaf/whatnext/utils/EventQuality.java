package com.asaf.whatnext.utils;

import com.asaf.whatnext.enums.PerformanceType;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.models.PerformingArt;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Tanıtım listelerinde (post, story, açıklama, Reel, popüler) yeri olmayan "amatör gece" etkinlikleri.
 *
 * <p>2026-09-16 canlı veri: İstanbul'daki 109 stand-up etkinliğinin (56 başlık) en sık tekrarlananları
 * isimsiz kulüp/mekân geceleriydi — "Kadıköy Stand-up Gecesi" (7×), "Kadıköy Komedi Kulübü Stand Up
 * Gecesi" (6×), "Stand up Açık Mikrofon Beyoğlu" (4×). Haftalık tekrarlandıkları için başlık-tekrar ve
 * tarih-yakınlığı puanı toplayıp isimli komedyenlerin (Anlatanadam, Kaan Sekban) önüne geçiyorlardı.
 */
public final class EventQuality {

    private EventQuality() {
    }

    /** Türü ne olursa olsun: açık mikrofon, quiz gecesi. */
    private static final Pattern ALWAYS_AMATEUR = Pattern.compile("\\b(acik mikrofon|open mic|quiz)\\b");

    /**
     * Yalnız stand-up'ta: isimsiz kulüp/mekân gecesi. Tiyatro ve konserde "gece" geçen gerçek yapımlar var
     * ("Bir Yaz Gecesi Rüyası"), o yüzden türle sınırlı.
     */
    private static final Pattern STANDUP_CLUB_NIGHT = Pattern.compile("\\b(gecesi|night|komedi kulubu|comedy club)\\b");

    /** Başlıktan bunlar ve mekân adı çıkınca hiçbir şey kalmıyorsa etkinlik isimsizdir ("Stand Up-The Harp Kadıköy"). */
    private static final Set<String> STANDUP_FILLER = Set.of(
            "stand", "up", "standup", "komedi", "comedy", "show", "sahne", "live", "istanbul",
            "kadikoy", "moda", "taksim", "beyoglu", "pera", "besiktas", "cihangir", "sisli",
            "bakirkoy", "uskudar", "atasehir", "maltepe");

    public static boolean isAmateurNight(Event event) {
        if (event == null) {
            return false;
        }
        boolean standup = event instanceof PerformingArt pa && pa.getPerformanceType() == PerformanceType.STANDUP;
        String venueName = event.getVenue() != null ? event.getVenue().getName() : null;
        return isAmateurNight(event.getTitle(), venueName, standup);
    }

    public static boolean isAmateurNight(String title, String venueName, boolean standup) {
        String t = fold(title);
        if (t.isEmpty()) {
            return false;
        }
        if (ALWAYS_AMATEUR.matcher(t).find()) {
            return true;
        }
        if (!standup) {
            return false;
        }
        if (STANDUP_CLUB_NIGHT.matcher(t).find()) {
            return true;
        }
        Set<String> venueTokens = Arrays.stream(fold(venueName).split(" ")).collect(Collectors.toSet());
        return Arrays.stream(t.split(" ")).allMatch(tok -> STANDUP_FILLER.contains(tok) || venueTokens.contains(tok));
    }

    /** Küçük harf + noktalama → boşluk + Türkçe harfler ASCII ("MIC" tr küçük harfte "mıc" olmasın diye). */
    public static String fold(String s) {
        return EventDuplicateKeys.normalizeTitle(s)
                .replace('ı', 'i').replace('ğ', 'g').replace('ü', 'u')
                .replace('ş', 's').replace('ö', 'o').replace('ç', 'c');
    }
}
