package com.asaf.whatnext.utils;

import com.asaf.whatnext.enums.PerformanceType;
import com.asaf.whatnext.models.PerformingArt;
import com.asaf.whatnext.models.Venue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Başlık/mekân çiftleri 2026-09-16 prod verisinden (İstanbul stand-up, 16–30 Eylül). */
class EventQualityTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '"', value = {
            "Kadıköy Stand-up Gecesi | Ada Bar Kadıköy",
            "Kadıköy Komedi Kulübü Stand Up Gecesi | Vohu Sahne",
            "STAND UP GECESİ Taksim- Pera- Beyoğlu | Efsahne Beyoğlu",
            "Stand up Taksim / Beyoğlu Gecesi | İnfiniti Sahne",
            "Stand up Açık Mikrofon Beyoğlu | İnfiniti Sahne",
            "Kadıköy Falan Stand Up Gecesi | Motto Sahne Kadıköy",
            "Kadıköy Açık Mikrofon Stand Up | Corner Kadıköy",
            "Stand up Bekarlar Gecesi | Corner Kadıköy",
            "English Stand Up Night | Vohu Sahne",
            "Kadıköy English Stand-up Comedy Open Mic | Dunia Kadıköy",
            "Boğaziçi Komedi Kulübü: Kadıköy Açık Mikrofon Stand-up Gecesi | Dunia Kadıköy",
            "Stand Up-The Harp Kadıköy | The Harp Sahne",
            "Stand-Up Menü Komedi Gecesi | Rose Marine Stage Cihangir",
            "Şokopop Quiz Night | JJ Pub Kanyon",
    })
    void clubNightsAndOpenMics_areAmateur(String title, String venue) {
        assertThat(EventQuality.isAmateurNight(title, venue, true)).as(title).isTrue();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '"', value = {
            "İsmail Nuri - Stand Up (Yeni Gösteri) | Corner Kadıköy",
            "Mustafa Sağır İle Stand Up | Sahne Beylerbeyi",
            "Serkan Dilik ile Stand-up Show | Milena Pub & Bistro",
            "Hamdi Apalı Tek Kişilik Stand Up | Corner Kadıköy",
            "Stand up Barış Balkır ve Zafer Erbil İkili | İnfiniti Sahne",
            "Anlatanadam | Torium Sahne",
            "TuzBiber 4'lü | Bonus Parkorman",
            "Gömercin Kuşları - Kaan Sekban & Ayşe Balıbey | Habitat Hilltown",
            "Pınar Fidan Stand Up | Moda Sahnesi",
            "Gökhan Ünver - Çok Tanıdık - Stand Up | HOP Sahne - Bakırköy",
    })
    void namedComedians_areNotAmateur(String title, String venue) {
        assertThat(EventQuality.isAmateurNight(title, venue, true)).as(title).isFalse();
    }

    @Test
    void nightRuleOnlyForStandup_butOpenMicEverywhere() {
        assertThat(EventQuality.isAmateurNight("Bir Yaz Gecesi Rüyası", "Harbiye Muhsin Ertuğrul", false)).isFalse();
        assertThat(EventQuality.isAmateurNight("90'lar Türkçe Pop Gecesi", "Blind İstanbul", false)).isFalse();
        assertThat(EventQuality.isAmateurNight("Akustik Open Mic", "Karga", false)).isTrue();
        assertThat(EventQuality.isAmateurNight(null, null, true)).isFalse();
    }

    @Test
    void entityOverload_usesPerformanceTypeAndVenue() {
        PerformingArt standup = new PerformingArt();
        standup.setTitle("Stand Up");
        standup.setPerformanceType(PerformanceType.STANDUP);
        Venue venue = new Venue();
        venue.setName("The Harp Sahne");
        standup.setVenue(venue);
        assertThat(EventQuality.isAmateurNight(standup)).isTrue();

        PerformingArt theater = new PerformingArt();
        theater.setTitle("Kadıköy Gecesi");
        theater.setPerformanceType(PerformanceType.THEATER);
        assertThat(EventQuality.isAmateurNight(theater)).isFalse();
    }
}
