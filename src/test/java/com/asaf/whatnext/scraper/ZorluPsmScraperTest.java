package com.asaf.whatnext.scraper;

import com.asaf.whatnext.enums.AudienceType;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class ZorluPsmScraperTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 6);

    // POST /Activity/Search kartından kısaltılmış (2026-11-06)
    private static final String CARD = """
            <div class="event-list-card">
              <div class="event-list-card-content">
                <a href="/etkinlikler/mix-festival-2026?p=fb35"><img src="/medium/Activity/Image/05de8d09" /></a>
                <div class="event-list-card-item">
                  <div class="event-list-card-item-header"><div>
                    <a href="/etkinlikler/festival"><span class="btn label">FESTİVAL</span></a>
                    <a href="/etkinlikler/hediye-kart-kampanya"><span class="btn label">HEDİYE KART</span></a>
                  </div></div>
                  <div class="event-list-card-item-detail">
                    <a href="/etkinlikler/mix-festival-2026?p=fb35" class="event-list-card-item-detail-text">
                      <h2 class="font-size-l">MIX FESTIVAL BY MODUNDAYIZ</h2></a>
                    <div class="location col-location"><p class="date">06 KASIM</p><b class="slash">/ </b><b class="hour">17:00</b></div>
                    <div class="location place"><p>%LABEL%</p></div>
                    <div class="event-list-card-item-detail-buttons">
                      <a href="/etkinlikler/mix-festival-2026?p=fb35" class="btn btn-primary">DETAYLI BİLGİ</a>
                      <a href="https://www.passo.com.tr/tr/etkinlik/mix-festival-2026-zorlupsm-passo/12382319" class="btn btn-secondary">BİLET AL</a>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            """;

    private static Element card(String place, String labels) {
        String html = CARD.replace("%LABEL%", place);
        if (labels != null) {
            html = html.replace("<span class=\"btn label\">FESTİVAL</span>", labels);
        }
        return Jsoup.parse(html).selectFirst("div.event-list-card");
    }

    @Test
    void mapsListCard() {
        ScrapedEventData d = ZorluPsmScraper.toEventData(card("ZORLU PSM", null), TODAY);

        assertThat(d.getTitle()).isEqualTo("MIX FESTIVAL BY MODUNDAYIZ");
        assertThat(d.getStartDate()).isEqualTo(LocalDate.of(2026, 11, 6));
        assertThat(d.getStartTime()).isEqualTo(LocalTime.of(17, 0));
        assertThat(d.getVenueName()).isEqualTo("Zorlu PSM");
        assertThat(d.getTicketUrl()).isEqualTo("https://www.passo.com.tr/tr/etkinlik/mix-festival-2026-zorlupsm-passo/12382319");
        assertThat(d.getImageUrl()).isEqualTo("https://www.zorlupsm.com/medium/Activity/Image/05de8d09");
        assertThat(d.getEventType()).isEqualTo(EventType.CONCERT);
        assertThat(d.getSource()).isEqualTo(EventSourceType.ZORLU_PSM);
        assertThat(d.getCity()).isEqualTo("istanbul");
    }

    @Test
    void hallNameAndLabelPriority() {
        ScrapedEventData d = ZorluPsmScraper.toEventData(card("TURKCELL SAHNESİ",
                "<span class=\"btn label\">TİYATRO</span><span class=\"btn label\">ÇOCUK</span>"
                        + "<span class=\"btn label\">STAND-UP</span>"), TODAY);
        assertThat(d.getVenueName()).isEqualTo("Zorlu PSM - Turkcell Sahnesi");
        assertThat(d.getEventType()).isEqualTo(EventType.STANDUP);
        assertThat(d.getAudience()).isEqualTo(AudienceType.CHILDREN);
    }

    @Test
    void skipsWorkshops() {
        assertThat(ZorluPsmScraper.toEventData(card("%100 STUDIO",
                "<span class=\"btn label\">ATÖLYE</span>"), TODAY)).isNull();
    }

    @Test
    void yearRollsOverForPastMonths() {
        assertThat(ZorluPsmScraper.parseDayMonth("15 OCAK", TODAY)).isEqualTo(LocalDate.of(2027, 1, 15));
        assertThat(ZorluPsmScraper.parseDayMonth("20 EKİM", TODAY)).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(ZorluPsmScraper.parseDayMonth("03 NİSAN", TODAY)).isEqualTo(LocalDate.of(2027, 4, 3));
        assertThat(ZorluPsmScraper.parseDayMonth("12 ŞUBAT", TODAY)).isEqualTo(LocalDate.of(2027, 2, 12));
    }
}
