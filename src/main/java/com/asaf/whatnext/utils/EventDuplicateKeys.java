package com.asaf.whatnext.utils;

import com.asaf.whatnext.models.Event;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Aynı gün + aynı etkinlik (başlık varyasyonları: büyük/küçük harf, noktalama, "FoodieFest" / "Foodiefest")
 * için tek anahtar; Instagram görselleri ve Reel listelerinde çift satırı önler.
 */
public final class EventDuplicateKeys {

    private EventDuplicateKeys() {
    }

    /**
     * NFC, tr-lower, U+0307 temizli\u011fi, alfasay\u0131sal d\u0131\u015f\u0131 \u2192 bo\u015fluk. Tek do\u011fruluk kayna\u011f\u0131 \u2014
     * {@link EventIdentity} da ayn\u0131 normalizasyonu kullan\u0131r.
     */
    public static String normalizeTitle(String title) {
        String t = title == null ? "" : title;
        t = Normalizer.normalize(t.trim(), Normalizer.Form.NFC);
        t = t.toLowerCase(Locale.forLanguageTag("tr-TR"));
        t = t.replace("\u0307", "");
        t = t.replaceAll("[^\\p{L}\\p{N}]+", " ");
        t = t.replaceAll("\\s+", " ").trim();
        return t;
    }

    public static String titleAndDateKey(String title, LocalDate startDate) {
        String t = normalizeTitle(title);
        String d = startDate != null ? startDate.toString() : "";
        return t + "|" + d;
    }

    public static String titleAndDateKey(Event e) {
        if (e == null) {
            return "|";
        }
        return titleAndDateKey(e.getTitle(), e.getStartDate());
    }

    /**
     * İlk görünen sırayı korur (repo / skor sıralamasındaki ilk kayıt kalır).
     */
    public static List<Event> dedupeByTitleAndDateKeepOrder(List<Event> in) {
        if (in == null || in.isEmpty()) {
            return in;
        }
        Set<String> seen = new HashSet<>();
        List<Event> out = new ArrayList<>();
        for (Event e : in) {
            if (e == null) {
                continue;
            }
            if (seen.add(titleAndDateKey(e))) {
                out.add(e);
            }
        }
        return out;
    }

    /**
     * Story görseli gibi insertion-order setlerde: aynı başlık+tarih ikinci kez gelirse çıkarılır.
     */
    public static void dedupeSetByTitleAndDate(Set<Event> set) {
        if (set == null || set.isEmpty()) {
            return;
        }
        Set<String> seen = new HashSet<>();
        set.removeIf(e -> !seen.add(titleAndDateKey(e)));
    }
}
