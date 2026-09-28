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
    void shareWeekendCarousel_hookFirst_thenLatestDayPostsInDayAndTypeOrder() throws Exception {
        LocalDate friday = LocalDate.of(2026, 10, 2);
        var oldFriConcert = img(1, "2026-10-02", "concert", true);
        var friConcert = img(5, "2026-10-02", "concert", true);
        var satTheater = img(3, "2026-10-03", "theater", false);
        var sunCover = img(4, "2026-10-04", "cover", false);
        when(instagramImageRepository.findByCreationDateBetween(friday, friday.plusDays(2)))
                .thenReturn(List.of(satTheater, oldFriConcert, sunCover, friConcert));
        var hook = img(9, "2026-10-02", "weekend", false);
        when(instagramService.createImage("weekend", "cover", null, friday)).thenReturn(hook);
        when(eventService.findByDate(anyString())).thenReturn(List.of());

        String result = instagramSchedulerService.shareWeekendCarousel(friday);

        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(instagramService).sharePost(captor.capture(), org.mockito.ArgumentMatchers.contains("Bu hafta sonu ne yapsak?"));
        assertThat(captor.getValue()).containsExactly(hook, friConcert, satTheater);
        assertThat(result).contains("3 görsel");
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
