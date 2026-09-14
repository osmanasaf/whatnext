package com.asaf.whatnext.scraper.biletix;

import com.asaf.whatnext.utils.EventDuplicateKeys;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Biletix'in kendi sıralı listelerinden ("çok satanlar" ve "öne çıkanlar") kurulan popülerlik dizini.
 *
 * <p>Solr belgelerinde gerçek bir popülerlik alanı yok (2026-09-14 canlı: yalnız {@code vote}=0);
 * sitenin ana sayfası bu listeleri {@code /wbtxapi/.../homePage/...} uçlarından sıralı çekiyor.
 * Liste elemanı ya tek etkinlik ({@code event.eventCode} = Solr {@code id}) ya da etkinlik grubu
 * ({@code group.name} ≈ Solr {@code group}/{@code parent}/{@code artist}/{@code sname}).
 * Canlı ölçüm (İstanbul, genişletilmiş alt kategorilerle): konser çok satanlarının 59/67'si,
 * sanat çok satanlarının 19/24'ü kazınan belgelerle eşleşti.
 */
public final class BiletixPopularityIndex {

    public static final BiletixPopularityIndex EMPTY = new BiletixPopularityIndex(Map.of(), Map.of());

    /** Editoryal "öne çıkanlar" satış sırası değil — çok satanların alt kademesinden biraz düşük. */
    static final int FEATURED_POINTS = 8;

    private final Map<String, Integer> byEventCode;
    private final Map<String, Integer> byName;

    private BiletixPopularityIndex(Map<String, Integer> byEventCode, Map<String, Integer> byName) {
        this.byEventCode = byEventCode;
        this.byName = byName;
    }

    /**
     * @param hotTicketResponses çok satanlar yanıtları (liste sırası = satış sırası)
     * @param featuredResponses  öne çıkanlar yanıtları (sıra önemsiz, sabit puan)
     */
    public static BiletixPopularityIndex from(List<JsonNode> hotTicketResponses, List<JsonNode> featuredResponses) {
        Map<String, Integer> byEventCode = new HashMap<>();
        Map<String, Integer> byName = new HashMap<>();
        for (JsonNode response : hotTicketResponses) {
            int rank = 0;
            for (JsonNode element : response.path("data").path("elements")) {
                addElement(element, pointsForRank(++rank), byEventCode, byName);
            }
        }
        for (JsonNode response : featuredResponses) {
            for (JsonNode element : response.path("data").path("elements")) {
                addElement(element, FEATURED_POINTS, byEventCode, byName);
            }
        }
        return new BiletixPopularityIndex(byEventCode, byName);
    }

    /** Sıra → puan: ilk 10 → 20, 11–25 → 15, 26–50 → 10, sonrası → 6. */
    static int pointsForRank(int rank) {
        if (rank <= 10) {
            return 20;
        }
        if (rank <= 25) {
            return 15;
        }
        if (rank <= 50) {
            return 10;
        }
        return 6;
    }

    private static void addElement(JsonNode element, int points,
                                   Map<String, Integer> byEventCode, Map<String, Integer> byName) {
        JsonNode event = element.path("event");
        if (!event.isMissingNode() && !event.isNull()) {
            putMax(byEventCode, event.path("eventCode").asText("").trim(), points);
            putMax(byName, EventDuplicateKeys.normalizeTitle(event.path("eventGroupName").asText("")), points);
            putMax(byName, EventDuplicateKeys.normalizeTitle(event.path("eventName").asText("")), points);
        }
        JsonNode group = element.path("group");
        if (!group.isMissingNode() && !group.isNull()) {
            putMax(byName, EventDuplicateKeys.normalizeTitle(group.path("name").asText("")), points);
        }
    }

    private static void putMax(Map<String, Integer> map, String key, int points) {
        if (!key.isEmpty()) {
            map.merge(key, points, Math::max);
        }
    }

    public boolean isEmpty() {
        return byEventCode.isEmpty() && byName.isEmpty();
    }

    public int size() {
        return byEventCode.size() + byName.size();
    }

    /**
     * "Blind İstanbul Konserleri", "BKM Etkinlikleri Maximum Uniq" gibi mekân/festival serisi grupları çok
     * satanlarda üst sıralarda, ama tek bir sanatçı değil: canlı probe (2026-09-14) Blind'daki 60+ küçük
     * konserin her birine 20 puan yazdı. Bir ad {@value #MAX_TITLES_FOR_ACT}'ten fazla farklı başlığa
     * eşleşiyorsa seri sayılır ve {@value #SERIES_POINTS} puana iner. Aynı sanatçının birden çok gecesi
     * (tek başlık, "Mabel Matiz" ×4) ve etkinlik koduyla doğrudan eşleşmeler etkilenmez.
     */
    public BiletixPopularityIndex dampSeries(List<JsonNode> solrDocs) {
        Map<String, Set<String>> titlesByName = new HashMap<>();
        for (JsonNode doc : solrDocs) {
            String title = normalizedTitle(doc);
            for (String name : matchingNames(doc, title)) {
                titlesByName.computeIfAbsent(name, k -> new HashSet<>()).add(title);
            }
        }
        Map<String, Integer> damped = new HashMap<>(byName);
        titlesByName.forEach((name, titles) -> {
            if (titles.size() > MAX_TITLES_FOR_ACT) {
                damped.merge(name, SERIES_POINTS, Math::min);
            }
        });
        return new BiletixPopularityIndex(byEventCode, damped);
    }

    static final int MAX_TITLES_FOR_ACT = 3;
    static final int SERIES_POINTS = 4;

    /** Solr belgesinin popülerlik puanı (0–20); listelerde yoksa 0. */
    public int pointsFor(JsonNode solrDoc) {
        int best = byEventCode.getOrDefault(solrDoc.path("id").asText("").trim(), 0);
        for (String name : matchingNames(solrDoc, normalizedTitle(solrDoc))) {
            best = Math.max(best, byName.get(name));
        }
        return best;
    }

    /** Belgenin group/parent/artist/sname alanlarıyla birebir ya da çok kelimeli önekle eşleşen liste adları. */
    private Set<String> matchingNames(JsonNode solrDoc, String title) {
        Set<String> names = new HashSet<>();
        for (String field : List.of("group", "parent", "artist")) {
            for (JsonNode value : solrDoc.path(field)) {
                String name = EventDuplicateKeys.normalizeTitle(value.asText(""));
                if (byName.containsKey(name)) {
                    names.add(name);
                }
            }
        }
        if (!title.isEmpty()) {
            for (String name : byName.keySet()) {
                if (isMultiWordPrefix(name, title) || name.equals(title)) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    private static String normalizedTitle(JsonNode solrDoc) {
        return EventDuplicateKeys.normalizeTitle(textOf(solrDoc.path("sname")));
    }

    /**
     * "zengin mutfağı turne" ↔ "zengin mutfağı" gibi kelime sınırında önek eşleşmesi. Kısa taraf en az
     * iki kelime olmalı — tek kelimelik grup adları ("Cimri", "Model") başka başlıkların önekine
     * yanlışlıkla oturmasın.
     */
    static boolean isMultiWordPrefix(String a, String b) {
        String shorter = a.length() <= b.length() ? a : b;
        String longer = shorter == a ? b : a;
        if (shorter.isEmpty() || shorter.indexOf(' ') < 0) {
            return false;
        }
        return longer.equals(shorter) || longer.startsWith(shorter + " ");
    }

    private static String textOf(JsonNode node) {
        return node.isArray() ? node.path(0).asText("") : node.asText("");
    }
}
