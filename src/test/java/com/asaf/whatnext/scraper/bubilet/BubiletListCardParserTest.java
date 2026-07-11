package com.asaf.whatnext.scraper.bubilet;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BubiletListCardParserTest {

    private final BubiletListCardParser parser = new BubiletListCardParser();

    private static String fixture(String name) {
        try {
            Path path = Path.of("src/test/resources/fixtures/bubilet", name);
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void parsesNewGridSingleVenueCardWithSlugAndPrice() {
        var parsed = parser.parse(fixture("card-single-venue.html"));

        assertThat(parsed).isPresent();
        var d = parsed.get();
        assertThat(d.title()).isEqualTo("The Imperial Russian Ballet Company / Swan Lake-Kuğu Gölü Balesi");
        assertThat(d.venueName()).isEqualTo("Harbiye Cemil Topuzlu Açıkhava Tiyatrosu");
        assertThat(d.venueSlug()).isEqualTo("harbiye-cemil-topuzlu-acikhava-tiyatrosu");
        assertThat(d.dateText()).isEqualTo("17 Temmuz Cum 21:00");
        assertThat(d.minPriceTl()).isEqualTo(1712);
        assertThat(d.multiVenue()).isFalse();
        assertThat(d.ticketUrl()).isEqualTo(
                "https://www.bubilet.com.tr/istanbul/etkinlik/the-imperial-russian-ballet-company-swan-lake-kugu-golu-balesi");
        assertThat(d.imageUrl()).startsWith("https://cdn.bubilet.com.tr/");
    }

    @Test
    void parsesNewGridMultiVenueCardAsVenueList() {
        var parsed = parser.parse(fixture("card-multi-venue.html"));

        assertThat(parsed).isPresent();
        var d = parsed.get();
        assertThat(d.title()).isEqualTo("Ölü'n Bizi Ayırana Dek Oyunu");
        assertThat(d.multiVenue()).isTrue();
        assertThat(d.venueNames()).containsExactly(
                "Kadıköy Selamiçeşme Özgürlük Parkı Amfi Tiyatro", "Trump Sahne");
        assertThat(d.venueSlug()).isNull();
        assertThat(d.dateText()).isEqualTo("16 Ağustos, 24 Ağustos");
        assertThat(d.minPriceTl()).isEqualTo(1250);
        assertThat(BubiletDateRules.requiresDetailPage(d.dateText())).isTrue();
    }

    @Test
    void parsesNewGridVenueNameContainingMonthWordWithoutMisreadingAsDate() {
        var parsed = parser.parse(fixture("card-venue-with-month-word.html"));

        assertThat(parsed).isPresent();
        var d = parsed.get();
        assertThat(d.venueName()).isEqualTo("Mayıs Sahnesi");
        assertThat(d.venueSlug()).isEqualTo("mayis-sahnesi");
        assertThat(d.multiVenue()).isFalse();
        assertThat(d.dateText()).isEqualTo("12 Eylül Cts 20:00");
        assertThat(d.minPriceTl()).isEqualTo(850);
    }

    @Test
    void parsesNewGridKidsTaggedCard() {
        var parsed = parser.parse(fixture("card-kids.html"));

        assertThat(parsed).isPresent();
        var d = parsed.get();
        assertThat(d.title()).isEqualTo("Karlar Ülkesi Yıldıztaşı Kolyesi");
        assertThat(d.venueName()).isEqualTo("Hilltown Seyirlik Sahne");
        assertThat(d.venueSlug()).isEqualTo("hilltown-seyirlik-sahne");
        assertThat(d.dateText()).isEqualTo("01 Ağustos Cts 16:00");
        assertThat(d.minPriceTl()).isEqualTo(400);
    }

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

    @Test
    void parsesElectroSessionsGridCardWithBabylon() {
        String html = """
                <a class="group flex h-full flex-col" href="/istanbul/etkinlik/electro-sessions">
                  <div class="flex flex-col overflow-hidden px-1 pt-1.5 h-[96px] sm:h-[104px]">
                    <div class="min-h-0 min-w-0">
                      <h3 class="line-clamp-2 text-[12px] font-semibold">Electro Sessions</h3>
                      <p class="mt-0.5 w-full truncate text-xs text-gray-500">Babylon</p>
                      <p class="mt-1 w-full truncate text-xs text-gray-500">02 Mayıs Cts 21:30</p>
                    </div>
                  </div>
                </a>
                """;

        var parsed = parser.parse(html);

        assertThat(parsed).isPresent();
        assertThat(parsed.get().title()).isEqualTo("Electro Sessions");
        assertThat(parsed.get().venueName()).isEqualTo("Babylon");
        assertThat(parsed.get().dateText()).isEqualTo("02 Mayıs Cts 21:30");
    }
}
