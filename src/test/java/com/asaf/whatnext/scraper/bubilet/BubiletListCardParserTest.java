package com.asaf.whatnext.scraper.bubilet;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BubiletListCardParserTest {

    private final BubiletListCardParser parser = new BubiletListCardParser();

    @Test
    void parsesSingleDateGridCardWithoutDetailNeed() {
        String html = """
                <a class="group flex h-full flex-col" title="Can Bonomo | Midterm Camp Fest" href="/istanbul/etkinlik/can-bonomo-midterm-camp-fest">
                  <img src="https://cdn.bubilet.com.tr/files/can-bonomo.png">
                  <div class="flex flex-col overflow-hidden px-1 pt-1.5 h-[96px]">
                    <div class="min-h-0 min-w-0">
                      <h3>Can Bonomo | Midterm Camp Fest</h3>
                      <p class="mt-0.5 w-full truncate text-xs leading-tight text-gray-500">Life Park</p>
                      <p class="mt-1 w-full truncate text-xs text-gray-500">25 Nisan Cts 20:00</p>
                    </div>
                  </div>
                </a>
                """;

        var parsed = parser.parse(html);

        assertThat(parsed).isPresent();
        assertThat(parsed.get().title()).isEqualTo("Can Bonomo | Midterm Camp Fest");
        assertThat(parsed.get().venueName()).isEqualTo("Life Park");
        assertThat(parsed.get().dateText()).isEqualTo("25 Nisan Cts 20:00");
        assertThat(parsed.get().ticketUrl()).isEqualTo("https://www.bubilet.com.tr/istanbul/etkinlik/can-bonomo-midterm-camp-fest");
        assertThat(BubiletDateRules.requiresDetailPage(parsed.get().dateText())).isFalse();
    }

    @Test
    void parsesMultiDateGridCardAndMarksDetailNeed() {
        String html = """
                <a class="group flex h-full flex-col" title="Ebru Gündeş “Beraber Söylüyoruz”" href="/istanbul/etkinlik/ebru-gundes">
                  <div class="min-h-0 min-w-0">
                    <h3>Ebru Gündeş “Beraber Söylüyoruz”</h3>
                    <p class="mt-0.5 w-full truncate text-xs leading-tight text-gray-500">Volkswagen Arena</p>
                    <p class="mt-1 w-full truncate text-xs text-gray-500">08 Mayıs, 09 Mayıs</p>
                  </div>
                </a>
                """;

        var parsed = parser.parse(html);

        assertThat(parsed).isPresent();
        assertThat(parsed.get().venueName()).isEqualTo("Volkswagen Arena");
        assertThat(parsed.get().dateText()).isEqualTo("08 Mayıs, 09 Mayıs");
        assertThat(BubiletDateRules.requiresDetailPage(parsed.get().dateText())).isTrue();
    }

    @Test
    void keepsCommaSeparatedVenueAsVenueWhenDateIsSecondLine() {
        String html = """
                <a class="group flex h-full flex-col" title="Baturay Özdemir Stand Up" href="/istanbul/etkinlik/baturay-ozdemir-stand-up">
                  <div class="min-h-0 min-w-0">
                    <h3>Baturay Özdemir Stand Up</h3>
                    <p class="mt-0.5 w-full truncate text-xs leading-tight text-gray-500">Caddebostan Kültür Merkezi Büyük Salon, DasDas - Ataşehir</p>
                    <p class="mt-1 w-full truncate text-xs text-gray-500">17 Mayıs, 06 Haziran</p>
                  </div>
                </a>
                """;

        var parsed = parser.parse(html);

        assertThat(parsed).isPresent();
        assertThat(parsed.get().venueName()).isEqualTo("Caddebostan Kültür Merkezi Büyük Salon, DasDas - Ataşehir");
        assertThat(parsed.get().dateText()).isEqualTo("17 Mayıs, 06 Haziran");
        assertThat(BubiletDateRules.requiresDetailPage(parsed.get().dateText())).isTrue();
    }

    @Test
    void keepsSameDayMultipleHoursOnList() {
        assertThat(BubiletDateRules.requiresDetailPage("22 Nisan Çar - 19:00, 21:30")).isFalse();
    }
}
