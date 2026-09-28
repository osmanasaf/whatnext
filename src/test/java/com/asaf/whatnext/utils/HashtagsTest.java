package com.asaf.whatnext.utils;

import com.asaf.whatnext.enums.EventType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HashtagsTest {

    private static final LocalDate SATURDAY = LocalDate.of(2026, 10, 3);
    private static final LocalDate TUESDAY = LocalDate.of(2026, 9, 29);

    @Test
    void build_personalizesByTypeDistrictAndPerformer_max5() {
        String tags = Hashtags.build(List.of(
                new Hashtags.Item(EventType.CONCERT, "Emre Aydın", "IF Performance Hall Beşiktaş", SATURDAY),
                new Hashtags.Item(EventType.CONCERT, "Soner Arıca", "Jolly Joker Kartal", SATURDAY),
                new Hashtags.Item(EventType.CONCERT, "Derya Bedavacı", "Jolly Joker Kartal", SATURDAY),
                new Hashtags.Item(EventType.THEATER, "Cyrano de Bergerac (Cyrano Rock)", "Zorlu PSM", SATURDAY)));

        // Beşiktaş (IF + Zorlu) ile Kartal 2-2 berabere → ilk görülen kazanır.
        assertThat(tags).isEqualTo("#istanbuletkinlikleri #istanbulkonser #beşiktaş #emreaydın #istanbultiyatro");
    }

    @Test
    void build_fewSignals_padsWithGenericTags_andAddsWeekend() {
        String tags = Hashtags.build(List.of(
                new Hashtags.Item(EventType.THEATER, "Kel Diva", "Bilinmeyen Sahne", SATURDAY)));

        // Tiyatro başlığı sanatçı sayılmaz; semt yok → hafta sonu + jenerik dolgu.
        assertThat(tags).isEqualTo("#istanbuletkinlikleri #istanbultiyatro #haftasonuistanbul #istanbul #etkinlik");
    }

    @Test
    void build_noEvents_stillReturnsFiveGenericTags() {
        assertThat(Hashtags.build(List.of()).split(" ")).hasSize(5);
    }

    @Test
    void districtTag_usesAliasWhenNameHasNoDistrict() {
        assertThat(Hashtags.districtTag("Harbiye Cemil Topuzlu Açıkhava")).isEqualTo("#şişli");
        assertThat(Hashtags.districtTag("Bostancı Gösteri Merkezi")).isEqualTo("#kadıköy");
        assertThat(Hashtags.districtTag("Hayal Kahvesi")).isNull();
    }

    @Test
    void performerTag_rejectsLongOrPunctuatedTitles() {
        assertThat(Hashtags.performerTag("Emre Aydın")).isEqualTo("#emreaydın");
        assertThat(Hashtags.performerTag("Kadıköy Stand-up Gecesi")).isNull();
        assertThat(Hashtags.performerTag("Redd")).isNull();
        assertThat(Hashtags.districtTag(null)).isNull();
        assertThat(Hashtags.build(List.of(
                new Hashtags.Item(EventType.STANDUP, "Cem Yılmaz", null, TUESDAY)))).contains("#cemyılmaz");
    }
}
