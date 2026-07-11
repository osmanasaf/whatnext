package com.asaf.whatnext.utils;

import com.asaf.whatnext.dto.EventSummaryDTO;
import com.asaf.whatnext.models.ConcertEvent;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.models.Venue;
import com.asaf.whatnext.scraper.ScrapedEventData;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Kayıt anında (ve mevcut veri temizliğinde) siteler arası aynı-etkinlik kimliği.
 * Çağıran taraf adayların zaten aynı {@code startDate} (ve şehir) içinden geldiğini garanti eder
 * (bkz. {@code EventRepository.findByStartDateAndCityWithVenue}) — burada sadece başlık/mekân/sanatçı kıyaslanır.
 *
 * Kademeler:
 * 1. Normalize başlık birebir eşit → true
 * 2. Mekân eşleşiyor (ikisinde de varsa slug, yoksa normalize mekân adı) VE başlık Jaccard ≥ eşik → true
 * 3. Başlık Jaccard ≥ güçlü eşik → mekândan bağımsız true
 * 4. Konser: sanatçı token'ları diğer başlıkta tamamen içeriliyor VE mekân eşleşiyor → true
 */
public final class EventIdentity {

    private EventIdentity() {
    }

    public static boolean isSameEvent(Event candidate, ScrapedEventData data, double jaccardThreshold, double jaccardStrongThreshold) {
        if (candidate == null || data == null) {
            return false;
        }
        Venue v = candidate.getVenue();
        return isSameEventCore(
                candidate.getTitle(), v != null ? v.getSlug() : null, v != null ? v.getName() : null,
                data.getTitle(), data.getVenueSlug(), data.getVenueName(), data.getArtistNames(),
                jaccardThreshold, jaccardStrongThreshold);
    }

    /** Mevcut veri temizliği (Faz 3.3) için: iki kalıcı {@link Event} kaydını karşılaştırır. */
    public static boolean isSameEvent(Event a, Event b, double jaccardThreshold, double jaccardStrongThreshold) {
        if (a == null || b == null) {
            return false;
        }
        Venue va = a.getVenue();
        Venue vb = b.getVenue();
        String artistNames = b instanceof ConcertEvent concertB && concertB.getArtist() != null
                ? concertB.getArtist().getName() : null;
        return isSameEventCore(
                a.getTitle(), va != null ? va.getSlug() : null, va != null ? va.getName() : null,
                b.getTitle(), vb != null ? vb.getSlug() : null, vb != null ? vb.getName() : null, artistNames,
                jaccardThreshold, jaccardStrongThreshold);
    }

    /**
     * {@link EventSummaryDTO} karşılaştırması (ör. haftalık Reel'de birden çok kategori listesinden
     * gelen aynı etkinliği eşlemek için). DTO'da venueSlug/artistNames yok — 4. kademe (sanatçı token
     * içerme) burada uygulanamaz, sadece başlık/mekân-adı kademeleri (1-3) çalışır.
     */
    public static boolean isSameEvent(EventSummaryDTO a, EventSummaryDTO b, double jaccardThreshold, double jaccardStrongThreshold) {
        if (a == null || b == null) {
            return false;
        }
        return isSameEventCore(
                a.getTitle(), null, a.getVenueName(),
                b.getTitle(), null, b.getVenueName(), null,
                jaccardThreshold, jaccardStrongThreshold);
    }

    private static boolean isSameEventCore(
            String titleA, String venueSlugA, String venueNameA,
            String titleB, String venueSlugB, String venueNameB, String artistNamesB,
            double jaccardThreshold, double jaccardStrongThreshold) {
        String normA = EventDuplicateKeys.normalizeTitle(titleA);
        String normB = EventDuplicateKeys.normalizeTitle(titleB);
        if (normA.isBlank() || normB.isBlank()) {
            return false;
        }
        if (normA.equals(normB)) {
            return true;
        }

        double jaccard = titleJaccard(normA, normB);
        boolean venueMatches = venueMatches(venueSlugA, venueNameA, venueSlugB, venueNameB);

        if (venueMatches && jaccard >= jaccardThreshold) {
            return true;
        }
        if (jaccard >= jaccardStrongThreshold) {
            return true;
        }
        return venueMatches && artistTokensContained(normA, artistNamesB);
    }

    private static boolean venueMatches(String slugA, String nameA, String slugB, String nameB) {
        if (slugA != null && !slugA.isBlank() && slugB != null && !slugB.isBlank()) {
            return slugA.equalsIgnoreCase(slugB);
        }
        String normA = EventDuplicateKeys.normalizeTitle(nameA);
        String normB = EventDuplicateKeys.normalizeTitle(nameB);
        if (normA.isBlank() || normB.isBlank()) {
            return false;
        }
        return normA.equals(normB);
    }

    private static boolean artistTokensContained(String candidateNormalizedTitle, String artistNames) {
        String normalizedArtist = EventDuplicateKeys.normalizeTitle(artistNames);
        if (normalizedArtist.isBlank()) {
            return false;
        }
        Set<String> artistTokens = tokenize(normalizedArtist);
        Set<String> candidateTokens = tokenize(candidateNormalizedTitle);
        return !artistTokens.isEmpty() && candidateTokens.containsAll(artistTokens);
    }

    private static double titleJaccard(String normalizedA, String normalizedB) {
        Set<String> ta = tokenize(normalizedA);
        Set<String> tb = tokenize(normalizedB);
        if (ta.isEmpty() || tb.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(ta);
        intersection.retainAll(tb);
        Set<String> union = new HashSet<>(ta);
        union.addAll(tb);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private static Set<String> tokenize(String normalized) {
        if (normalized == null || normalized.isBlank()) {
            return Set.of();
        }
        return new HashSet<>(Arrays.asList(normalized.split(" ")));
    }
}
