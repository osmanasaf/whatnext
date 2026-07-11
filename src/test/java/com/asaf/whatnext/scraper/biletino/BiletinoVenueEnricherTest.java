package com.asaf.whatnext.scraper.biletino;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BiletinoVenueEnricherTest {

    @Test
    void realDetailPageTitle_extractsVenue() {
        // Canlı denetimde doğrulandı (2026-07-11): https://biletino.com/en/e-1bp6/feridun-duzagac/
        String title = "FERİDUN DÜZAĞAÇ | 11 July 2026 | Dorock XL | Biletino";
        assertThat(BiletinoVenueEnricher.extractVenueFromTitle(title)).isEqualTo("Dorock XL");
    }

    @Test
    void titleWithPipeInEventName_stillExtractsLastVenueSegment() {
        String title = "Chambord + Orkun Bozdemir + Opposition | 11 July 2026 | Klein Phönix | Biletino";
        assertThat(BiletinoVenueEnricher.extractVenueFromTitle(title)).isEqualTo("Klein Phönix");
    }

    @Test
    void unexpectedFormat_returnsNull() {
        assertThat(BiletinoVenueEnricher.extractVenueFromTitle("Search Page | Biletino")).isNull();
        assertThat(BiletinoVenueEnricher.extractVenueFromTitle("")).isNull();
        assertThat(BiletinoVenueEnricher.extractVenueFromTitle(null)).isNull();
    }
}
