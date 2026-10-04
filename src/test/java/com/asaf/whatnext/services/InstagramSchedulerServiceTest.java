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
    @Mock
    private com.asaf.whatnext.repository.InstagramImageRepository instagramImageRepository;
    @Mock
    private InstagramService instagramService;
    @Mock
    private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

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

    private static com.asaf.whatnext.models.InstagramImage img(long id, String date, String eventType, boolean posted) {
        com.asaf.whatnext.models.InstagramImage im = new com.asaf.whatnext.models.InstagramImage();
        im.setId(id);
        im.setCreationDate(LocalDate.parse(date));
        im.setEventType(eventType);
        im.setPostType("cover".equals(eventType) || "weekend".equals(eventType) ? "cover" : "post");
        im.setPostedToInstagram(posted);
        return im;
    }

    @Test
    @SuppressWarnings("unchecked")
    void shareWeekendCarousel_rendersHook_threeDays_picks_cta_inOrder_skippingEmpty() throws Exception {
        LocalDate friday = LocalDate.of(2026, 10, 2);
        when(instagramImageRepository.findByCreationDateBetween(friday, friday.plusDays(2))).thenReturn(List.of());
        var hook = img(1, "2026-10-02", "weekend", false);
        var fri = img(2, "2026-10-02", "mixed", false);
        var sun = img(4, "2026-10-04", "mixed", false);
        var picks = img(5, "2026-10-02", "picks", false);
        var cta = img(6, "2026-10-02", "cta", false);
        when(instagramService.createImage("weekend", "cover", null, friday)).thenReturn(hook);
        when(instagramService.createImage("mixed", "post", "istanbul", friday)).thenReturn(fri);
        when(instagramService.createImage("mixed", "post", "istanbul", friday.plusDays(1))).thenReturn(null); // etkinlik yok
        when(instagramService.createImage("mixed", "post", "istanbul", friday.plusDays(2))).thenReturn(sun);
        when(instagramService.createImage("picks", "cover", "istanbul", friday)).thenReturn(picks);
        when(instagramService.createImage("cta", "cover", "istanbul", friday)).thenReturn(cta);
        when(eventService.findByDate(anyString())).thenReturn(List.of());
        when(transactionTemplate.execute(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> ((org.springframework.transaction.support.TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));

        String result = instagramSchedulerService.shareWeekendCarousel(friday);

        // 2026-10-01: caption cron thread'inde oturumsuz kuruluyordu (LazyInitializationException, Venue).
        org.mockito.Mockito.verify(transactionTemplate).execute(org.mockito.ArgumentMatchers.any());

        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(instagramService).sharePost(captor.capture(), org.mockito.ArgumentMatchers.contains("Bu hafta sonu ne yapsak?"));
        assertThat(captor.getValue()).containsExactly(hook, fri, sun, picks, cta);
        assertThat(result).contains("5 görsel");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shareDailyPost_todayHook_thenConcertTheaterStandup_evenIfWeeklyCoverAlreadyPosted() throws Exception {
        LocalDate day = LocalDate.of(2026, 10, 6);
        // Pazartesi karuseli bu günün "cover"ını paylaşmış olabilir — günlük gönderiyi engellememeli.
        when(instagramImageRepository.findByCreationDateBetween(day, day)).thenReturn(List.of(img(1, "2026-10-06", "cover", true)));
        var hook = img(2, "2026-10-06", "today", false);
        var c = img(3, "2026-10-06", "concert", false);
        var s = img(5, "2026-10-06", "standup", false);
        when(instagramService.createImage("today", "cover", null, day)).thenReturn(hook);
        when(instagramService.createImage("concert", "post", "istanbul", day)).thenReturn(c);
        when(instagramService.createImage("theater", "post", "istanbul", day)).thenReturn(null);
        when(instagramService.createImage("standup", "post", "istanbul", day)).thenReturn(s);
        when(eventService.findByDate(anyString())).thenReturn(List.of());
        when(transactionTemplate.execute(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> ((org.springframework.transaction.support.TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));

        instagramSchedulerService.shareDailyPost(day, false);

        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(instagramService).sharePost(captor.capture(), org.mockito.ArgumentMatchers.contains("Bugün İstanbul'da"));
        assertThat(captor.getValue()).containsExactly(hook, c, s);
    }

    @Test
    void shareWeekendCarousel_skipsWhenHookAlreadyPosted() throws Exception {
        LocalDate friday = LocalDate.of(2026, 10, 2);
        when(instagramImageRepository.findByCreationDateBetween(friday, friday.plusDays(2)))
                .thenReturn(List.of(img(9, "2026-10-02", "weekend", true)));

        assertThat(instagramSchedulerService.shareWeekendCarousel(friday)).contains("zaten paylaşılmış");
        org.mockito.Mockito.verifyNoInteractions(instagramService);
    }
}
