package com.asaf.whatnext.services;

import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.enums.PerformanceType;
import com.asaf.whatnext.models.ConcertEvent;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.models.PerformingArt;
import com.asaf.whatnext.repository.EventRepository;
import com.asaf.whatnext.services.scoring.EventScoringStrategy;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 9, 15);

    private final EventRepository repository = mock(EventRepository.class);

    private EventService serviceWithScores(Map<String, Integer> scores) {
        EventScoringStrategy scoring = e -> scores.getOrDefault(e.getTitle(), 0);
        return new EventService(repository, null, null, null, scoring);
    }

    private static <T extends Event> T titled(T e, long id, String title) {
        e.setId(id);
        e.setTitle(title);
        e.setStartDate(DATE);
        return e;
    }

    private static PerformingArt performingArt(long id, String title, PerformanceType type) {
        PerformingArt pa = titled(new PerformingArt(), id, title);
        pa.setPerformanceType(type);
        return pa;
    }

    /** Repository startDate/id sırasıyla döner — popüler etkinlik sonda olsa bile limit onu kesmemeli. */
    @Test
    void topN_sortsByScoreBeforeLimit() {
        when(repository.findByTypeAndIstanbulActiveOnDate(EventType.CONCERT, DATE)).thenReturn(List.of(
                titled(new ConcertEvent(), 1, "Erken Kazınan A"),
                titled(new ConcertEvent(), 2, "Erken Kazınan B"),
                titled(new ConcertEvent(), 3, "Popüler Konser")));

        List<Event> top = serviceWithScores(Map.of("Popüler Konser", 20))
                .findTopNEventsForDateAndCity(DATE, EventType.CONCERT, "istanbul", 1);

        assertThat(top).extracting(Event::getTitle).containsExactly("Popüler Konser");
    }

    @Test
    void topN_standupFiltersPerformingArtsAndSorts() {
        when(repository.findByTypeAndIstanbulActiveOnDate(EventType.PERFORMING_ART, DATE)).thenReturn(List.of(
                performingArt(1, "Oyun", PerformanceType.THEATER),
                performingArt(2, "Az Bilinen Stand-up", PerformanceType.STANDUP),
                performingArt(3, "Cem Yılmaz", PerformanceType.STANDUP)));

        List<Event> top = serviceWithScores(Map.of("Cem Yılmaz", 15, "Oyun", 30))
                .findTopNEventsForDateAndCity(DATE, EventType.STANDUP, "istanbul", 6);

        assertThat(top).extracting(Event::getTitle).containsExactly("Cem Yılmaz", "Az Bilinen Stand-up");
    }
}
