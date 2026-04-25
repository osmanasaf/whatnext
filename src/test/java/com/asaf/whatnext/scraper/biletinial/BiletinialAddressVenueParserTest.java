package com.asaf.whatnext.scraper.biletinial;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BiletinialAddressVenueParserTest {

    @Test
    void prefersSmallTag() {
        String html = """
                <address>
                  <b>İstanbul (Avr.)</b>
                  <br>
                  <small>AKM Türk Telekom Oditoryumu</small>
                </address>
                """;
        assertThat(BiletinialAddressVenueParser.extractVenueNameFromAddressHtml(html))
                .isEqualTo("AKM Türk Telekom Oditoryumu");
    }

    @Test
    void fallsBackToTextWithoutBoldCity() {
        String html = """
                <address>
                  <b>İstanbul</b>
                  Harbiye Açık Hava
                </address>
                """;
        assertThat(BiletinialAddressVenueParser.extractVenueNameFromAddressHtml(html).trim())
                .contains("Harbiye");
    }
}
