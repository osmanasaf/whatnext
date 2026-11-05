package com.asaf.whatnext.scraper;

import com.asaf.whatnext.enums.AudienceType;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class PassoScraperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // POST allevents {"CityId":101,...} kaydından kısaltılmış (2026-11-02)
    private static final String ITEM = """
            {"id":12315321,"date":"2026-11-07T17:30:00","endDate":"2026-11-07T22:30:00",
             "name":"Emre Altuğ \\"Bir Pop Masalı\\"","eventType":0,"hideHour":false,
             "venueName":"Zorlu PSM - İstanbul",
             "homePageImagePath":"31082026152150-emre.jpeg?w=760&h=400",
             "seoUrl":"emre-altug-bir-pop-masali-zorlu-psm"}
            """;

    @Test
    void mapsAllEventsItem() throws Exception {
        ScrapedEventData d = PassoScraper.toEventData(MAPPER.readTree(ITEM), EventType.CONCERT, "istanbul", false);

        assertThat(d.getTitle()).isEqualTo("Emre Altuğ \"Bir Pop Masalı\"");
        assertThat(d.getStartDate()).isEqualTo(LocalDate.of(2026, 11, 7));
        assertThat(d.getStartTime()).isEqualTo(LocalTime.of(17, 30));
        assertThat(d.getEndDate()).isNull();
        assertThat(d.getVenueName()).isEqualTo("Zorlu PSM");
        assertThat(d.getCity()).isEqualTo("istanbul");
        assertThat(d.getTicketUrl()).isEqualTo("https://www.passo.com.tr/tr/etkinlik/emre-altug-bir-pop-masali-zorlu-psm/12315321");
        assertThat(d.getImageUrl()).isEqualTo("https://image.passo.com.tr/api/r/tr/p/event/31082026152150-emre.jpeg");
        assertThat(d.getSource()).isEqualTo(EventSourceType.PASSO);
        assertThat(d.getAudience()).isNull();
    }

    @Test
    void childrenSubgenreAndHiddenHour() throws Exception {
        String json = ITEM.replace("\"hideHour\":false", "\"hideHour\":true");
        ScrapedEventData d = PassoScraper.toEventData(MAPPER.readTree(json), EventType.THEATER, "istanbul", true);
        assertThat(d.getStartTime()).isNull();
        assertThat(d.getAudience()).isEqualTo(AudienceType.CHILDREN);
    }

    @Test
    void rejectsPlaceholderDate() throws Exception {
        String json = ITEM.replace("2026-11-07T17:30:00", "0001-01-01T00:00:00");
        assertThat(PassoScraper.toEventData(MAPPER.readTree(json), EventType.CONCERT, "istanbul", false)).isNull();
    }

    @Test
    void stripsCitySuffixFromVenue() {
        assertThat(PassoScraper.cleanVenue("Şerefiye Sarnıcı | İstanbul")).isEqualTo("Şerefiye Sarnıcı");
        assertThat(PassoScraper.cleanVenue("Zorlu PSM - Turkcell Sahnesi")).isEqualTo("Zorlu PSM - Turkcell Sahnesi");
        assertThat(PassoScraper.cleanVenue("")).isNull();
    }
}
