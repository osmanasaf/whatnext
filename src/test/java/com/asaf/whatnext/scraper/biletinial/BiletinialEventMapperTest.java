package com.asaf.whatnext.scraper.biletinial;

import com.asaf.whatnext.enums.Biletinial.BiletinialCategory;
import com.asaf.whatnext.scraper.ScrapedEventData;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class BiletinialEventMapperTest {

    @Test
    void fromDetailSession_carriesStartTimeFromSchemaOrgIsoDatetime() {
        // Canlı denetimde doğrulandı (2026-07-11): time[itemprop='startDate'] content="2026-07-11T21:00"
        BiletinialCardDraft draft = new BiletinialCardDraft(
                "BEAT - Performing the Music of 80s KING CRIMSON",
                "https://biletinial.com/tr-tr/muzik/beat-konseri",
                null, "Harbiye Cemil Topuzlu Açıkhava Sahnesi", "İstanbul Avrupa",
                BiletinialCategory.MUSIC, "istanbul-avrupa", LocalDate.of(2026, 7, 11));

        ScrapedEventData data = BiletinialEventMapper.fromDetailSession(
                draft, LocalDate.of(2026, 7, 11), LocalTime.of(21, 0),
                "11 Temmuz Cumartesi, 21:00", "Harbiye Cemil Topuzlu Açıkhava Sahnesi");

        assertThat(data.getStartDate()).isEqualTo(LocalDate.of(2026, 7, 11));
        assertThat(data.getStartTime()).isEqualTo(LocalTime.of(21, 0));
    }

    @Test
    void fromListOnly_hasNoStartTime() {
        BiletinialCardDraft draft = new BiletinialCardDraft(
                "Gülinler", "https://biletinial.com/tr-tr/muzik/gulinler",
                null, "Blind İstanbul", "İstanbul Avrupa",
                BiletinialCategory.MUSIC, "istanbul-avrupa", LocalDate.of(2026, 7, 11));

        ScrapedEventData data = BiletinialEventMapper.fromListOnly(draft);

        assertThat(data.getStartTime()).isNull();
    }
}
