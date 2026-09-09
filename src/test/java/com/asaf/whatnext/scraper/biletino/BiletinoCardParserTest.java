package com.asaf.whatnext.scraper.biletino;

import com.asaf.whatnext.enums.Biletino.BiletinoCategory;
import com.asaf.whatnext.enums.Biletino.BiletinoCity;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.scraper.ScrapedEventData;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kart HTML'i canlı sayfadan alınmıştır (2026-09-09). Tarayıcı gerekmediğinden parser artık
 * doğrudan Jsoup ile test edilebiliyor.
 */
class BiletinoCardParserTest {

    private static final String CARD_HTML = """
            <div class="col-md-4 product">
              <div class="card mb-4 shadow-sm">
                <a href="/en/e-1d27/kadinlar-matinesi/" class="card-image event-url">
                  <div class="event-image img-container">
                    <img src="https://resources-biletino.s3.eu-west-1.amazonaws.com/content/event/63583/480x270.jpg">
                  </div>
                </a>
                <a href="/en/e-1d27/kadinlar-matinesi/" class="card-body event-url">
                  <h3 class="title">Kadinlar Matinesi Zenne ve Oryantal Show</h3>
                  <p class="card-text date">09 September Wednesday</p>
                  <p class="card-text sub-date">Today</p>
                  <p class="card-text location">Basaksehir, İstanbul</p>
                </a>
              </div>
            </div>
            """;

    private final BiletinoCardParser parser = new BiletinoCardParser();

    private Element card(String html) {
        return Jsoup.parse(html).selectFirst("div.col-md-4.product");
    }

    @Test
    void kartiAyristirir() {
        ScrapedEventData data = parser.parseCard(card(CARD_HTML), BiletinoCategory.MUSIC, BiletinoCity.ISTANBUL);

        assertThat(data.getTitle()).isEqualTo("Kadinlar Matinesi Zenne ve Oryantal Show");
        assertThat(data.getStartDate().getMonthValue()).isEqualTo(9);
        assertThat(data.getStartDate().getDayOfMonth()).isEqualTo(9);
        assertThat(data.getVenueName()).isEqualTo("Basaksehir");
        assertThat(data.getCity()).isEqualTo("istanbul");
        assertThat(data.getEventType()).isEqualTo(EventType.CONCERT);
        assertThat(data.getTicketUrl()).isEqualTo("https://biletino.com/en/e-1d27/kadinlar-matinesi/");
        assertThat(data.getImageUrl()).contains("480x270.jpg");
    }

    /** Şehir kartın kendi alanından gelir; arama şehri varsayılmaz. */
    @Test
    void baskaSehirdekiKartAramaSehriniDevralmaz() {
        ScrapedEventData data = parser.parseCard(
                card(CARD_HTML.replace("Basaksehir, İstanbul", "Cankaya, Ankara")),
                BiletinoCategory.MUSIC, BiletinoCity.ISTANBUL);

        assertThat(data.getCity()).isEqualTo("ankara");
    }

    @Test
    void baslikYoksaNullDoner() {
        assertThat(parser.parseCard(card(CARD_HTML.replace("Kadinlar Matinesi Zenne ve Oryantal Show", "")),
                BiletinoCategory.MUSIC, BiletinoCity.ISTANBUL)).isNull();
    }
}
