package com.asaf.whatnext.scraper.biletix;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BiletixCardParserTest {

    private final BiletixCardParser parser = new BiletixCardParser();

    @Test
    void singleDateWithTime() {
        String s = "08 Eyl 2026, 21:00";
        assertEquals(LocalDate.of(2026, 9, 8), parser.parseBiletixDateString(s));
        assertEquals(LocalTime.of(21, 0), parser.parseBiletixTime(s));
        assertNull(parser.parseEndDate(s));
    }

    @Test
    void dateRange() {
        String s = " 07 Eyl 2026 - 30 Kas 2026 ";
        assertEquals(LocalDate.of(2026, 9, 7), parser.parseBiletixDateString(s));
        assertEquals(LocalDate.of(2026, 11, 30), parser.parseEndDate(s));
        assertNull(parser.parseBiletixTime(s));
    }

    @Test
    void dateOnlyAndGarbage() {
        assertEquals(LocalDate.of(2026, 5, 12), parser.parseBiletixDateString("12 May 2026"));
        assertEquals(LocalDate.of(2026, 3, 1), parser.parseBiletixDateString("01 Mar 2026"));
        assertNull(parser.parseBiletixDateString("Yakında"));
        assertNull(parser.parseBiletixDateString(null));
    }
}
