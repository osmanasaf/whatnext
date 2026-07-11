package com.asaf.whatnext.services;

import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.models.ConcertEvent;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.models.Venue;
import com.asaf.whatnext.services.scoring.EventScoringStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstagramSchedulerServiceTest {

    @Mock
    private EventService eventService;
    @Mock
    private EventScoringStrategy scoringStrategy;

    @InjectMocks
    private InstagramSchedulerService instagramSchedulerService;

    private static ConcertEvent concert(String title, String city, LocalTime time, String venueName) {
        ConcertEvent e = new ConcertEvent();
        e.setTitle(title);
        e.setCity(city);
        e.setStartTime(time);
        e.setSource(EventSourceType.BUBILET);
        e.setType(EventType.CONCERT);
        if (venueName != null) {
            Venue v = new Venue();
            v.setName(venueName);
            e.setVenue(v);
        }
        return e;
    }

    @Test
    void buildDailyCaption_listsEventsWithTimeAndVenue_andHasSaveCta() {
        LocalDate date = LocalDate.of(2026, 7, 17);
        Event withTime = concert("Feridun Düzağaç", "istanbul", LocalTime.of(21, 0), "Dorock XL Kadıköy");
        Event noTime = concert("Gülinler", "istanbul", null, null);
        Event ankara = concert("Ankara Konseri", "ankara", LocalTime.of(20, 0), "Salon");

        when(eventService.findByDate(date.toString())).thenReturn(List.of(withTime, noTime, ankara));
        when(scoringStrategy.calculateScore(org.mockito.ArgumentMatchers.any())).thenReturn(0);

        String caption = instagramSchedulerService.buildDailyCaption(date);

        assertThat(caption).contains("Feridun Düzağaç");
        assertThat(caption).contains("21:00");
        assertThat(caption).contains("Dorock XL Kadıköy");
        assertThat(caption).contains("Gülinler");
        assertThat(caption).doesNotContain("Ankara Konseri");
        assertThat(caption).contains("Kaydet");
        assertThat(caption).contains("#istanbul");
    }

    @Test
    void buildDailyCaption_noEvents_fallsBackToGenericLine() {
        LocalDate date = LocalDate.of(2026, 7, 18);
        when(eventService.findByDate(anyString())).thenReturn(List.of());

        String caption = instagramSchedulerService.buildDailyCaption(date);

        assertThat(caption).contains("Kaçırma");
        assertThat(caption).doesNotContain("Kaydet, hafta içinde");
    }
}
