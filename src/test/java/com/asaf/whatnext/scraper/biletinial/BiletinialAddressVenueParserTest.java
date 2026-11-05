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
    void hpv2CardAddressIsVenueText() {
        String html = """
                <address>
                  <span class="hpv2-event-card__meta-icon"><svg viewBox="0 0 24 24"><circle cx="12" cy="10" r="2.5" /></svg></span>
                  <span>Caddebostan K&#252;lt&#252;r Merkezi B&#252;y&#252;k Salon</span>
                </address>
                """;
        assertThat(BiletinialAddressVenueParser.extractVenueNameFromAddressHtml(html))
                .isEqualTo("Caddebostan Kültür Merkezi Büyük Salon");
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
