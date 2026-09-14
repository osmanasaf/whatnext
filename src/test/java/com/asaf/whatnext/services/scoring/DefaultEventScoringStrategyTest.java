package com.asaf.whatnext.services.scoring;

import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.enums.PerformanceType;
import com.asaf.whatnext.models.Artist;
import com.asaf.whatnext.models.ConcertEvent;
import com.asaf.whatnext.models.PerformingArt;
import com.asaf.whatnext.models.Venue;
import com.asaf.whatnext.services.TitlePopularityService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultEventScoringStrategyTest {

    private final TitlePopularityService titles = mock(TitlePopularityService.class);
    private final DefaultEventScoringStrategy strategy = new DefaultEventScoringStrategy(titles, new PopularityCatalog());

    {
        when(titles.calculateTitlePopularityScore(anyString(), any())).thenReturn(0);
    }

    /** Tarih bonusu 0 kalsın diye uzak tarih. */
    private static final LocalDate FAR = LocalDate.now().plusDays(30);

    @Test
    void standupGetsCatalogVenueAndSourcePopularity() {
        PerformingArt standup = new PerformingArt();
        standup.setTitle("Cem Yılmaz - Diamond Elite Platinum Plus");
        standup.setType(EventType.PERFORMING_ART);
        standup.setPerformanceType(PerformanceType.STANDUP);
        standup.setStartDate(FAR);
        Venue venue = new Venue();
        venue.setName("Zorlu PSM Turkcell Sahnesi");
        standup.setVenue(venue);
        standup.setSourcePopularity(15);

        // katalog sanatçı 10 + katalog mekân 10 + Biletix çok satan 15
        assertThat(strategy.calculateScore(standup)).isEqualTo(35);
    }

    @Test
    void concertArtistUsesMaxOfDbAndCatalog_notSum() {
        ConcertEvent concert = new ConcertEvent();
        concert.setTitle("Şebnem Ferah");
        concert.setType(EventType.CONCERT);
        concert.setStartDate(FAR);
        Artist artist = new Artist();
        artist.setName("Şebnem Ferah");
        artist.setPopularityScore(5);
        concert.setArtist(artist);

        assertThat(strategy.calculateScore(concert)).isEqualTo(10);
    }

    @Test
    void unknownEventScoresZero() {
        ConcertEvent concert = new ConcertEvent();
        concert.setTitle("Mahalle Grubu Akustik");
        concert.setType(EventType.CONCERT);
        concert.setStartDate(FAR);

        assertThat(strategy.calculateScore(concert)).isZero();
    }
}
