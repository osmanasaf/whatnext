package com.asaf.whatnext.utils;

import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.models.ConcertEvent;
import com.asaf.whatnext.models.PerformingArt;
import com.asaf.whatnext.models.Venue;
import com.asaf.whatnext.scraper.ScrapedEventData;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class EventIdentityTest {

    private static final double JACCARD = 0.5;
    private static final double JACCARD_STRONG = 0.85;

    private static ScrapedEventData data(String title, String venueName, String venueSlug, String artistNames) {
        return ScrapedEventData.builder()
                .title(title)
                .ticketUrl("https://example.com/x")
                .startDate(LocalDate.of(2026, 8, 1))
                .venueName(venueName)
                .venueSlug(venueSlug)
                .artistNames(artistNames)
                .eventType(EventType.CONCERT)
                .source(EventSourceType.BUBILET)
                .build();
    }

    @Test
    void exactNormalizedTitleMatch_isSame() {
        PerformingArt candidate = new PerformingArt();
        candidate.setTitle("FoodieFest İstanbul");

        ScrapedEventData incoming = data("Foodiefest İstanbul", null, null, null);

        assertThat(EventIdentity.isSameEvent(candidate, incoming, JACCARD, JACCARD_STRONG)).isTrue();
    }

    @Test
    void sameVenueSlug_moderateTitleOverlap_isSame() {
        Venue venue = new Venue();
        venue.setSlug("zorlu-psm");
        venue.setName("Zorlu PSM");
        PerformingArt candidate = new PerformingArt();
        candidate.setTitle("Cem Yılmaz CMYLMZ");
        candidate.setVenue(venue);

        ScrapedEventData incoming = data("Cem Yılmaz CMYLMZ Yeni Gösteri", null, "zorlu-psm", null);

        assertThat(EventIdentity.isSameEvent(candidate, incoming, JACCARD, JACCARD_STRONG)).isTrue();
    }

    @Test
    void differentVenueSlug_lowOverlap_isNotSame() {
        Venue venue = new Venue();
        venue.setSlug("zorlu-psm");
        venue.setName("Zorlu PSM");
        PerformingArt candidate = new PerformingArt();
        candidate.setTitle("Cem Yılmaz CMYLMZ");
        candidate.setVenue(venue);

        ScrapedEventData incoming = data("Tamamen Farklı Bir Oyun", null, "baska-mekan", null);

        assertThat(EventIdentity.isSameEvent(candidate, incoming, JACCARD, JACCARD_STRONG)).isFalse();
    }

    @Test
    void veryHighTitleJaccard_isSameRegardlessOfVenue() {
        PerformingArt candidate = new PerformingArt();
        candidate.setTitle("Kral Lear Tiyatro Oyunu Sahne Uyarlaması");

        ScrapedEventData incoming = data("Kral Lear Tiyatro Oyunu Sahne Uyarlaması Yeni", "Farklı Mekan", null, null);

        assertThat(EventIdentity.isSameEvent(candidate, incoming, JACCARD, JACCARD_STRONG)).isTrue();
    }

    @Test
    void concertArtistTokensContainedAndVenueMatches_isSame() {
        Venue venue = new Venue();
        venue.setName("Volkswagen Arena");
        ConcertEvent candidate = new ConcertEvent();
        candidate.setTitle("Sezen Aksu Unutulmayan Şarkılar Konseri");
        candidate.setVenue(venue);

        ScrapedEventData incoming = data("Sezen Aksu", "Volkswagen Arena", null, "Sezen Aksu");

        assertThat(EventIdentity.isSameEvent(candidate, incoming, JACCARD, JACCARD_STRONG)).isTrue();
    }

    /** Eşik sınırı: düşük jaccard + mekân eşleşmesi yok + sanatçı yok → eşleşmemeli. */
    @Test
    void lowOverlapNoVenueNoArtist_isNotSame() {
        PerformingArt candidate = new PerformingArt();
        candidate.setTitle("Hamlet");

        ScrapedEventData incoming = data("Macbeth", null, null, null);

        assertThat(EventIdentity.isSameEvent(candidate, incoming, JACCARD, JACCARD_STRONG)).isFalse();
    }

    @Test
    void turkishCaseFolding_iAndDottedI_matchAsSameTitle() {
        PerformingArt candidate = new PerformingArt();
        candidate.setTitle("İstanbul Caz Festivali");

        ScrapedEventData incoming = data("istanbul caz festivali", null, null, null);

        assertThat(EventIdentity.isSameEvent(candidate, incoming, JACCARD, JACCARD_STRONG)).isTrue();
    }

    @Test
    void nullTitles_areNotSame() {
        PerformingArt candidate = new PerformingArt();
        candidate.setTitle(null);

        ScrapedEventData incoming = data("Herhangi Bir Şey", null, null, null);

        assertThat(EventIdentity.isSameEvent(candidate, incoming, JACCARD, JACCARD_STRONG)).isFalse();
    }
}
