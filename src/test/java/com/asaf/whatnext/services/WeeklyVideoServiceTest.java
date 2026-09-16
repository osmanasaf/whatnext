package com.asaf.whatnext.services;

import com.asaf.whatnext.dto.EventSummaryDTO;
import com.asaf.whatnext.dto.WeeklyPopularEventsDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyVideoServiceTest {

    /** Test tarihleri (Temmuz) bugünden önce; geçmiş-etkinlik filtresi sabit "bugün" ile test edilir. */
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 13);

    private WeeklyVideoService service;

    @BeforeEach
    void setUp() {
        service = new WeeklyVideoService();
        ReflectionTestUtils.setField(service, "jaccardThreshold", 0.5);
        ReflectionTestUtils.setField(service, "jaccardStrongThreshold", 0.85);
    }

    private static EventSummaryDTO event(Long id, String title, String venue, LocalDate date, LocalTime time) {
        EventSummaryDTO e = new EventSummaryDTO();
        e.setId(id);
        e.setTitle(title);
        e.setVenueName(venue);
        e.setStartDate(date);
        e.setStartTime(time);
        return e;
    }

    @Test
    void collectEventsForVideo_sortsByDateAndTime_notByCategory() {
        WeeklyPopularEventsDTO weekly = new WeeklyPopularEventsDTO();
        weekly.setConcerts(List.of(
                event(1L, "Pazar Konseri", "Zorlu PSM", LocalDate.of(2026, 7, 19), LocalTime.of(21, 0))));
        weekly.setTheaters(List.of(
                event(2L, "Salı Oyunu", "Şehir Tiyatrosu", LocalDate.of(2026, 7, 14), LocalTime.of(20, 0))));
        weekly.setStandups(List.of(
                event(3L, "Cuma Standup", "Kadıköy Sahne", LocalDate.of(2026, 7, 17), null)));

        List<WeeklyVideoService.SelectedEvent> result = service.collectEventsForVideo(weekly, TODAY);

        assertThat(result).extracting(se -> se.event().getTitle())
                .containsExactly("Salı Oyunu", "Cuma Standup", "Pazar Konseri");
    }

    @Test
    void collectEventsForVideo_dedupesAcrossCategoriesByEventIdentity() {
        WeeklyPopularEventsDTO weekly = new WeeklyPopularEventsDTO();
        LocalDate date = LocalDate.of(2026, 7, 17);
        weekly.setConcerts(List.of(
                event(1L, "FoodieFest İstanbul", "Zorlu PSM", date, LocalTime.of(21, 0))));
        // Farklı kaynaktan aynı etkinlik, biraz farklı başlık ama aynı mekân — EventIdentity eşlemeli.
        weekly.setTheaters(List.of(
                event(2L, "Foodiefest İstanbul", "Zorlu PSM", date, LocalTime.of(21, 0))));
        weekly.setStandups(List.of());

        List<WeeklyVideoService.SelectedEvent> result = service.collectEventsForVideo(weekly, TODAY);

        assertThat(result).hasSize(1);
    }

    @Test
    void collectEventsForVideo_marksFirstPerCategoryAsFeatured() {
        WeeklyPopularEventsDTO weekly = new WeeklyPopularEventsDTO();
        LocalDate date = LocalDate.of(2026, 7, 17);
        weekly.setConcerts(List.of(
                event(1L, "Konser A", "Mekan A", date, LocalTime.of(20, 0)),
                event(2L, "Konser B", "Mekan B", date, LocalTime.of(21, 0))));
        weekly.setTheaters(List.of());
        weekly.setStandups(List.of());

        List<WeeklyVideoService.SelectedEvent> result = service.collectEventsForVideo(weekly, TODAY);

        assertThat(result).hasSize(2);
        assertThat(result.stream().filter(WeeklyVideoService.SelectedEvent::featured)).hasSize(1);
        assertThat(result.stream().filter(WeeklyVideoService.SelectedEvent::featured).findFirst().get().event().getTitle())
                .isEqualTo("Konser A");
    }

    @Test
    void collectEventsForVideo_skipsZeroScoreAfterCategoryLeader() {
        WeeklyPopularEventsDTO weekly = new WeeklyPopularEventsDTO();
        LocalDate date = LocalDate.of(2026, 9, 19);
        EventSummaryDTO leader = event(1L, "Anlatanadam", "Torium Sahne", date, LocalTime.of(20, 30));
        leader.setPopularityScore(20);
        EventSummaryDTO unknown = event(2L, "Mahalle Komedi Akşamı", "Kafe Sahne", date, LocalTime.of(20, 15));
        unknown.setPopularityScore(0);
        EventSummaryDTO known = event(3L, "Gömercin Kuşları - Kaan Sekban", "Habitat Hilltown", date, LocalTime.of(20, 30));
        known.setPopularityScore(10);
        weekly.setConcerts(List.of());
        weekly.setTheaters(List.of());
        weekly.setStandups(List.of(leader, unknown, known));

        List<WeeklyVideoService.SelectedEvent> result = service.collectEventsForVideo(weekly, TODAY);

        assertThat(result).extracting(se -> se.event().getTitle())
                .containsExactlyInAnyOrder("Anlatanadam", "Gömercin Kuşları - Kaan Sekban");
    }

    /** 2026-09-16: hafta penceresi Pazartesiden başladığı için dünkü etkinlik (lider bile olsa) Reel'e giriyordu. */
    @Test
    void collectEventsForVideo_skipsPastEvents_evenAsLeader() {
        LocalDate today = LocalDate.of(2026, 9, 16);
        WeeklyPopularEventsDTO weekly = new WeeklyPopularEventsDTO();
        EventSummaryDTO yesterday = event(4L, "Meçhul Paşa", "Sahne Dragos", today.minusDays(1), LocalTime.of(20, 30));
        yesterday.setPopularityScore(24);
        EventSummaryDTO tonight = event(5L, "Zengin Mutfağı", "Maximum UNIQ Hall", today, LocalTime.of(20, 30));
        tonight.setPopularityScore(50);
        EventSummaryDTO comedian = event(3L, "Pınar Fidan Stand Up", "Moda Sahnesi", today.plusDays(4), LocalTime.of(20, 30));
        comedian.setPopularityScore(12);
        weekly.setConcerts(List.of());
        weekly.setTheaters(List.of(yesterday, tonight));
        weekly.setStandups(List.of(comedian));

        List<WeeklyVideoService.SelectedEvent> result = service.collectEventsForVideo(weekly, today);

        assertThat(result).extracting(se -> se.event().getTitle())
                .containsExactly("Zengin Mutfağı", "Pınar Fidan Stand Up");
    }

    @Test
    void buildReelCaption_listsEventsWithDayAndTime() {
        EventSummaryDTO withTime = event(1L, "Feridun Düzağaç", "Dorock XL", LocalDate.of(2026, 7, 17), LocalTime.of(21, 0));
        EventSummaryDTO noTime = event(2L, "Gülinler", "Blind İstanbul", LocalDate.of(2026, 7, 18), null);
        List<WeeklyVideoService.SelectedEvent> events = List.of(
                new WeeklyVideoService.SelectedEvent(withTime, true),
                new WeeklyVideoService.SelectedEvent(noTime, false));

        String caption = service.buildReelCaption("17-23 Temmuz", events);

        assertThat(caption).contains("Feridun Düzağaç");
        assertThat(caption).contains("Cuma");
        assertThat(caption).contains("21:00");
        assertThat(caption).contains("Gülinler");
        assertThat(caption).contains("Cumartesi");
        assertThat(caption).contains("Kaydet");
        assertThat(caption).contains("#istanbul");
    }
}
