package com.asaf.whatnext.scraper.biletino;

import com.asaf.whatnext.enums.Biletino.BiletinoCity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BiletinoLocationParserTest {

    @Test
    void commaSeparated_splitsVenueAndCity() {
        BiletinoLocationParser.VenueCity r = BiletinoLocationParser.parse(
                "Zorlu PSM, İstanbul", BiletinoCity.ISTANBUL);
        assertThat(r.venueName()).isEqualTo("Zorlu PSM");
        assertThat(r.city()).isEqualTo("İstanbul");
    }

    @Test
    void noComma_usesFullTextAsVenueAndSearchCity() {
        BiletinoLocationParser.VenueCity r = BiletinoLocationParser.parse(
                "Harbiye Cemil Topuzlu Açık Hava", BiletinoCity.ISTANBUL);
        assertThat(r.venueName()).isEqualTo("Harbiye Cemil Topuzlu Açık Hava");
        assertThat(r.city()).isEqualTo("istanbul");
    }

    @Test
    void lastPartAfterMultipleCommas_isCity() {
        BiletinoLocationParser.VenueCity r = BiletinoLocationParser.parse(
                "Salon, Blok, Ankara", BiletinoCity.ISTANBUL);
        assertThat(r.venueName()).isEqualTo("Salon");
        assertThat(r.city()).isEqualTo("Ankara");
    }
}
