package com.asaf.whatnext.utils;

import com.asaf.whatnext.enums.EventType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Paylaşımdaki etkinliklere göre hashtag seti: tür, semt (mekân adından), sanatçı, hafta sonu.
 * Eski sabit 12–20'lik jenerik liste (#istanbul, #galatatower...) milyonluk etiketlerde kayboluyordu;
 * Instagram da artık az ve alakalı etiketi öne çıkarıyor, bu yüzden en fazla {@link #MAX} etiket.
 */
public final class Hashtags {

    public record Item(EventType type, String title, String venueName, LocalDate date) {
    }

    static final int MAX = 5;
    private static final Locale TR = Locale.forLanguageTag("tr");
    private static final String BASE = "#istanbuletkinlikleri";
    private static final List<String> FILLER = List.of("#istanbul", "#etkinlik", "#istanbuldayasam", "#etkinliktakvimi");

    private static final Map<EventType, String> TYPE_TAGS = Map.of(
            EventType.CONCERT, "#istanbulkonser",
            EventType.THEATER, "#istanbultiyatro",
            EventType.STANDUP, "#standupistanbul",
            EventType.EXHIBITION, "#istanbulsergi",
            EventType.PERFORMING_ART, "#istanbulsahne");

    /** Mekân adında geçen semt; yoksa bilinen semt-dışı yer adı → semt. Sıra önemli: ilk eşleşen kazanır. */
    private static final List<String> DISTRICTS = List.of(
            "Kadıköy", "Beşiktaş", "Şişli", "Beyoğlu", "Üsküdar", "Ataşehir", "Maltepe", "Kartal", "Pendik",
            "Bakırköy", "Sarıyer", "Beylikdüzü", "Esenyurt", "Fatih", "Kağıthane", "Eyüpsultan", "Zeytinburnu",
            "Bahçelievler", "Başakşehir", "Ümraniye", "Büyükçekmece");
    private static final Map<String, String> PLACE_TO_DISTRICT = new LinkedHashMap<>();

    static {
        PLACE_TO_DISTRICT.put("bostancı", "Kadıköy");
        PLACE_TO_DISTRICT.put("caddebostan", "Kadıköy");
        PLACE_TO_DISTRICT.put("moda", "Kadıköy");
        PLACE_TO_DISTRICT.put("kozyatağı", "Kadıköy");
        PLACE_TO_DISTRICT.put("harbiye", "Şişli");
        PLACE_TO_DISTRICT.put("nişantaşı", "Şişli");
        PLACE_TO_DISTRICT.put("zorlu", "Beşiktaş");
        PLACE_TO_DISTRICT.put("levent", "Beşiktaş");
        PLACE_TO_DISTRICT.put("ortaköy", "Beşiktaş");
        PLACE_TO_DISTRICT.put("taksim", "Beyoğlu");
        PLACE_TO_DISTRICT.put("galata", "Beyoğlu");
        PLACE_TO_DISTRICT.put("karaköy", "Beyoğlu");
        PLACE_TO_DISTRICT.put("akm", "Beyoğlu");
        PLACE_TO_DISTRICT.put("maslak", "Sarıyer");
        PLACE_TO_DISTRICT.put("istinye", "Sarıyer");
        PLACE_TO_DISTRICT.put("mall of istanbul", "Başakşehir");
    }

    private Hashtags() {
    }

    /**
     * Öncelik: temel etiket, en sık tür, en sık semt, en popüler sanatçı, ikinci tür, ikinci sanatçı,
     * hafta sonu, ikinci semt — ilk {@link #MAX} farklı etiket. {@code items} önem sırasında verilmeli
     * (sanatçı etiketi listedeki ilk isimlerden seçilir).
     */
    public static String build(List<Item> items) {
        List<String> types = byFrequency(items, i -> i.type() == null ? null : TYPE_TAGS.get(i.type()));
        List<String> districts = byFrequency(items, i -> districtTag(i.venueName()));
        List<String> performers = items.stream()
                .filter(i -> i.type() == EventType.CONCERT || i.type() == EventType.STANDUP)
                .map(i -> performerTag(i.title()))
                .filter(t -> t != null)
                .distinct()
                .toList();
        boolean weekend = items.stream().anyMatch(i -> i.date() != null
                && (i.date().getDayOfWeek() == DayOfWeek.SATURDAY || i.date().getDayOfWeek() == DayOfWeek.SUNDAY));

        Set<String> tags = new LinkedHashSet<>();
        tags.add(BASE);
        addAt(tags, types, 0);
        addAt(tags, districts, 0);
        addAt(tags, performers, 0);
        addAt(tags, types, 1);
        addAt(tags, performers, 1);
        if (weekend) {
            tags.add("#haftasonuistanbul");
        }
        addAt(tags, districts, 1);
        tags.addAll(FILLER);
        return tags.stream().limit(MAX).collect(Collectors.joining(" "));
    }

    static String districtTag(String venueName) {
        if (venueName == null || venueName.isBlank()) {
            return null;
        }
        String v = venueName.toLowerCase(TR);
        for (String d : DISTRICTS) {
            if (v.contains(d.toLowerCase(TR))) {
                return "#" + d.toLowerCase(TR);
            }
        }
        for (Map.Entry<String, String> e : PLACE_TO_DISTRICT.entrySet()) {
            if (v.contains(e.getKey())) {
                return "#" + e.getValue().toLowerCase(TR);
            }
        }
        return null;
    }

    /**
     * Başlık bir kişi/grup adına benziyorsa (2–3 kelime, yalnız harf) etiket: "Emre Aydın" → #emreaydın.
     * "Cyrano de Bergerac (Cyrano Rock)", "Kadıköy Stand-up Gecesi" gibi uzun/noktalamalı başlıklar elenir.
     */
    static String performerTag(String title) {
        if (title == null) {
            return null;
        }
        String t = title.trim();
        String[] words = t.split("\\s+");
        if (words.length < 2 || words.length > 3 || t.length() > 25 || !t.matches("[\\p{L} ]+")) {
            return null;
        }
        return "#" + t.toLowerCase(TR).replace(" ", "");
    }

    private static List<String> byFrequency(List<Item> items, Function<Item, String> tagOf) {
        Map<String, Long> counts = items.stream()
                .map(tagOf)
                .filter(t -> t != null)
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
        List<String> out = new ArrayList<>(counts.keySet());
        out.sort(Comparator.comparing(counts::get).reversed());
        return out;
    }

    private static void addAt(Set<String> tags, List<String> list, int index) {
        if (list.size() > index) {
            tags.add(list.get(index));
        }
    }
}
