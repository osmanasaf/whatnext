package com.asaf.whatnext.scraper;

import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class BugeceScraperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // /api/list?indices=events&city=istanbul kaydından kısaltılmış (2026-11-02)
    private static final String HIT = """
            {"_id":"6a89ee5f88517765029a8a93","name":"ARENA FIGHT NIGHTS — AFN VOL.004",
             "date":"2026-11-21T14:00:00.000Z","start_time":"2026-11-21T14:00:00.000Z","end_time":"2026-11-21T21:00:00.000Z",
             "venue":{"name":"Ora Arena","slug":"ora-arena"},
             "slug":"arena-fight-nights-afn-vol004-11-21-26--5368d672",
             "image":"https://cdn.bugece.co/20f5b1a7-fac4-47ad-8340-eb9fe3a9e725",
             "city":{"name":"Istanbul"},
             "sections":[{"slug":"editorun-sectikleri-93c5j"},{"slug":"top-10-on-bugece-0k6zl"}],
             "starting_from":600}
            """;

    @Test
    void mapsListHit() throws Exception {
        ScrapedEventData d = BugeceScraper.toEventData(MAPPER.readTree(HIT));

        assertThat(d.getTitle()).isEqualTo("ARENA FIGHT NIGHTS — AFN VOL.004");
        assertThat(d.getStartDate()).isEqualTo(LocalDate.of(2026, 11, 21));
        assertThat(d.getStartTime()).isEqualTo(LocalTime.of(17, 0)); // UTC 14:00 → TR 17:00
        assertThat(d.getEndDate()).isNull();
        assertThat(d.getVenueName()).isEqualTo("Ora Arena");
        assertThat(d.getVenueSlug()).isEqualTo("ora-arena");
        assertThat(d.getCity()).isEqualTo("istanbul");
        assertThat(d.getTicketUrl()).isEqualTo("https://bugece.co/tr/event/arena-fight-nights-afn-vol004-11-21-26--5368d672");
        assertThat(d.getMinPriceTl()).isEqualTo(600);
        assertThat(d.getEventType()).isEqualTo(EventType.CONCERT);
        assertThat(d.getSource()).isEqualTo(EventSourceType.BUGECE);
        assertThat(d.getSourcePopularity()).isEqualTo(20);
        assertThat(d.isValid()).isTrue();
    }

    @Test
    void popularityFromSections() throws Exception {
        JsonNode editor = MAPPER.readTree("{\"sections\":[{\"slug\":\"editorun-sectikleri-93c5j\"}]}");
        JsonNode none = MAPPER.readTree("{\"sections\":[{\"slug\":\"istanbul-fveeh\"}]}");
        assertThat(BugeceScraper.popularityOf(editor)).isEqualTo(10);
        assertThat(BugeceScraper.popularityOf(none)).isZero();
    }

    @Test
    void rejectsHitWithoutDate() throws Exception {
        assertThat(BugeceScraper.toEventData(MAPPER.readTree("{\"name\":\"X\",\"slug\":\"x\"}"))).isNull();
    }
}
