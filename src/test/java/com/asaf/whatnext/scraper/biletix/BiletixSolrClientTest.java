package com.asaf.whatnext.scraper.biletix;

import com.asaf.whatnext.config.BiletixScraperConfig;
import com.asaf.whatnext.enums.Biletix.BiletixCategory;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.scraper.ScrapedEventData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BiletixSolrClientTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ScrapedEventData parse(String json, BiletixCategory category) throws Exception {
        return BiletixSolrClient.toEventData(MAPPER.readTree(json), category);
    }

    @Test
    void tekTarihliBelgeyiTurkiyeSaatiyleCevirir() throws Exception {
        ScrapedEventData data = parse("""
                {"id":"50A35","sname":"DJ Barış Sert","svenue":"Hayal Kahvesi Emaar ",
                 "city":["İstanbul"],"start":"2026-09-09T18:00:00Z","end":"2026-09-09T18:00:00Z",
                 "subcategory":"pop$MUSIC","region":"ISTANBUL","image_url":"50B0A-x.avif"}
                """, BiletixCategory.MUSIC);

        assertEquals(LocalDate.of(2026, 9, 9), data.getStartDate());
        assertEquals(LocalTime.of(21, 0), data.getStartTime());
        assertNull(data.getEndDate());
        assertEquals("Hayal Kahvesi Emaar", data.getVenueName());
        assertEquals("istanbul", data.getCity());
        assertEquals(EventType.CONCERT, data.getEventType());
        assertEquals("https://www.biletix.com/etkinlik/50A35/ISTANBUL/tr", data.getTicketUrl());
        assertEquals(BiletixScraperConfig.IMAGE_URL_PREFIX + "50B0A-x.avif", data.getImageUrl());
    }

    @Test
    void aralikliBelgedeBitisTarihiDolar() throws Exception {
        ScrapedEventData data = parse("""
                {"id":"5PA17","sname":"Chalet Garden","svenue":"Swissotel","city":["İstanbul"],
                 "start":"2026-09-09T16:30:00Z","end":"2026-09-30T16:30:00Z",
                 "subcategory":"pop$MUSIC","region":"ISTANBUL"}
                """, BiletixCategory.MUSIC);

        assertEquals(LocalDate.of(2026, 9, 9), data.getStartDate());
        assertEquals(LocalDate.of(2026, 9, 30), data.getEndDate());
        assertNull(data.getImageUrl());
    }

    @Test
    void sehirDisiBelgeIstanbulOlarakEtiketlenmez() throws Exception {
        ScrapedEventData data = parse("""
                {"id":"5XXXX","sname":"Antalya Konseri","svenue":"Bir Mekan","city":["Antalya"],
                 "start":"2026-09-09T18:00:00Z","end":"2026-09-09T18:00:00Z",
                 "subcategory":"pop$MUSIC","region":"ANTALYA"}
                """, BiletixCategory.MUSIC);

        assertEquals("antalya", data.getCity());
        assertEquals("https://www.biletix.com/etkinlik/5XXXX/ANTALYA/tr", data.getTicketUrl());
    }

    @Test
    void altKategoriEtkinlikTuruneCevrilir() {
        assertEquals(EventType.STANDUP, BiletixSolrClient.eventTypeOf("stand_up$ART", BiletixCategory.ART));
        assertEquals(EventType.THEATER, BiletixSolrClient.eventTypeOf("tiyatro$ART", BiletixCategory.ART));
        assertEquals(EventType.PERFORMING_ART, BiletixSolrClient.eventTypeOf("bale_dans$ART", BiletixCategory.ART));
        assertEquals(EventType.CONCERT, BiletixSolrClient.eventTypeOf("tiyatro$ART", BiletixCategory.MUSIC));
    }

    @Test
    void altKategoriFiltresiOrIleKurulur() {
        assertEquals("subcategory:(\"tiyatro$ART\" OR \"gosteri$ART\" OR \"bale_dans$ART\" OR \"stand_up$ART\")",
                BiletixScraperConfig.buildSubcategoryFilter(BiletixCategory.ART));
    }
}
