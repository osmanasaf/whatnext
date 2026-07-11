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

    /** 28 Aralık'ta "5 Ocak" scrape'i: geçen yıla değil, gelecek yıla düşmeli (yıl devri). */
    @Test
    void parseBubiletListDate_rollsOverToNextYearAtYearBoundary() {
        LocalDate now = LocalDate.of(2025, 12, 28);
        LocalDate d = TurkishDateParser.parseBubiletListDate("05 Ocak Pzt 20:00", now, 30);
        assertThat(d).isEqualTo(LocalDate.of(2026, 1, 5));
    }

    @Test
    void extractDayMonthFromTurkishText_rollsOverToNextYearAtYearBoundary() {
        LocalDate now = LocalDate.of(2025, 12, 28);
        List<LocalDate> dates = TurkishDateParser.extractDayMonthFromTurkishText("5 Ocak", now, 30);
        assertThat(dates).containsExactly(LocalDate.of(2026, 1, 5));
    }
}
