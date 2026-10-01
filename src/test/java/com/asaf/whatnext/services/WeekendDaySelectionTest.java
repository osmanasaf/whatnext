package com.asaf.whatnext.services;

import com.asaf.whatnext.dto.EventSummaryDTO;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.mapper.EventMapper;
import com.asaf.whatnext.models.ConcertEvent;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.services.scoring.EventScoringStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/** "Bu hafta sonu ne yapsak?" gün slaytı: 4 konser + 4 tiyatro + 2 stand-up, eksik tür dolgu, saat sırası. */
@ExtendWith(MockitoExtension.class)
class WeekendDaySelectionTest {

    private static final LocalDate SAT = LocalDate.of(2026, 10, 10);

    @Mock
    private EventService eventService;
    @Mock
    private EventScoringStrategy scoringStrategy;
    @Spy
    private EventMapper eventMapper = new EventMapper();
    @InjectMocks
    private ImageGenerationService service;

    private static List<Event> events(EventType type, String prefix, int count, int hourStart) {
        List<Event> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ConcertEvent e = new ConcertEvent();
            e.setId((long) (prefix.hashCode() * 100 + i));
            e.setTitle(prefix + i);
            e.setType(type);
            e.setCity("istanbul");
            e.setStartDate(SAT);
            e.setStartTime(LocalTime.of(hourStart + i, 0));
            out.add(e);
        }
        return out;
    }

    @Test
    void quota442_fillsMissingStandupFromOthers_sortedByTime() {
        // 6 konser, 6 tiyatro ama tek stand-up: 4 + 4 + 1 + 1 dolgu = 10.
        when(eventService.findTopNEventsForDateAndCity(eq(SAT), eq(EventType.CONCERT), any(), anyInt()))
                .thenReturn(events(EventType.CONCERT, "Konser", 6, 10));
        when(eventService.findTopNEventsForDateAndCity(eq(SAT), eq(EventType.THEATER), any(), anyInt()))
                .thenReturn(events(EventType.THEATER, "Oyun", 6, 11));
        when(eventService.findTopNEventsForDateAndCity(eq(SAT), eq(EventType.STANDUP), any(), anyInt()))
                .thenReturn(events(EventType.STANDUP, "Standup", 1, 9));
        lenient().when(scoringStrategy.calculateScore(any())).thenReturn(0);

        List<EventSummaryDTO> picked = service.selectWeekendDayEvents(SAT);

        assertThat(picked).hasSize(10);
        assertThat(picked.stream().filter(e -> e.getTitle().startsWith("Konser")).count()).isGreaterThanOrEqualTo(4);
        assertThat(picked.stream().filter(e -> e.getTitle().startsWith("Oyun")).count()).isGreaterThanOrEqualTo(4);
        assertThat(picked).extracting(EventSummaryDTO::getTitle).contains("Standup0");
        assertThat(picked).extracting(EventSummaryDTO::getStartTime).isSorted();
    }
}
