package com.asaf.whatnext.scraper.common;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TurkishDateParserTest {

    @Test
    void parseBubiletListDate_usesFirstSegment() {
        LocalDate fixed = LocalDate.of(2025, 6, 1);
        LocalDate d = TurkishDateParser.parseBubiletListDate("12 Aralık 2025 - 15 Aralık 2025", fixed, 30);
        assertThat(d).isNotNull();
        assertThat(d.getDayOfMonth()).isEqualTo(12);
        assertThat(d.getMonthValue()).isEqualTo(12);
    }

    @Test
    void extractDayMonthFromTurkishText_dedupesAndSorts() {
        String text = "28 Nisan Salı ve 12 Mayıs; ayrıca 28 Nisan Cum";
        List<LocalDate> dates = TurkishDateParser.extractDayMonthFromTurkishText(
                text, LocalDate.of(2025, 4, 1), 60);
        assertThat(dates).hasSize(2);
    }
}
