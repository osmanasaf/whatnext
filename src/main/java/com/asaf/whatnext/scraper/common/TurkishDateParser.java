package com.asaf.whatnext.scraper.common;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Türkçe ay adları, yıl rollover ve metin içi gün+ay eşleştirme.
 */
public final class TurkishDateParser {

    private static final Pattern TURKISH_DATE_IN_TEXT = Pattern.compile(
            "(\\d{1,2})\\s+(Ocak|Şubat|Mart|Nisan|Mayıs|Haziran|Temmuz|Ağustos|Eylül|Ekim|Kasım|Aralık)");

    /** Liste satırı içindeki saat: "17 Temmuz Cum 21:00" → 21:00. Nokta ayraç da kabul edilir. */
    private static final Pattern TIME_IN_TEXT = Pattern.compile("(\\d{1,2})[:.](\\d{2})");

    private TurkishDateParser() {
    }

    public static int monthNumberOrZero(String monthStr) {
        if (monthStr == null) {
            return 0;
        }
        return switch (monthStr) {
            case "Ocak" -> 1;
            case "Şubat" -> 2;
            case "Mart" -> 3;
            case "Nisan" -> 4;
            case "Mayıs" -> 5;
            case "Haziran" -> 6;
            case "Temmuz" -> 7;
            case "Ağustos" -> 8;
            case "Eylül" -> 9;
            case "Ekim" -> 10;
            case "Kasım" -> 11;
            case "Aralık" -> 12;
            default -> 0;
        };
    }

    /**
     * Liste satırı: "12 Şub" veya "12 Şub 2025 - 14 Şub" ilk parça.
     *
     * @param rolloverIfOlderThanDays bugünden N gün öncesi yılda yuvarlama
     */
    public static LocalDate parseBubiletListDate(
            String dateText,
            LocalDate now,
            int rolloverIfOlderThanDays) {
        if (dateText == null || dateText.isEmpty()) {
            return null;
        }
        try {
            String[] parts = dateText.split(" - ");
            String datePart = parts[0].trim();
            String[] tokens = datePart.split("\\s+");
            if (tokens.length < 2) {
                return null;
            }
            int day = Integer.parseInt(tokens[0]);
            int month = monthNumberOrZero(tokens[1]);
            if (month < 1) {
                return null;
            }
            int year = now.getYear();
            LocalDate date = LocalDate.of(year, month, day);
            if (date.isBefore(now.minusDays(rolloverIfOlderThanDays))) {
                date = date.plusYears(1);
            }
            return date;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Liste satırı: "17 Temmuz Cum 21:00" → 21:00. Saat yoksa (ör. "08 Mayıs, 09 Mayıs" veya
     * detaydan gelen düz {@code LocalDate.toString()}) {@code null} — zorlama sezgisel yok.
     */
    public static LocalTime parseBubiletListTime(String dateText) {
        if (dateText == null || dateText.isEmpty()) {
            return null;
        }
        try {
            String[] parts = dateText.split(" - ");
            String datePart = parts[0].trim();
            Matcher m = TIME_IN_TEXT.matcher(datePart);
            if (m.find()) {
                int hour = Integer.parseInt(m.group(1));
                int minute = Integer.parseInt(m.group(2));
                if (hour >= 0 && hour < 24 && minute >= 0 && minute < 60) {
                    return LocalTime.of(hour, minute);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * "Biletler" bölümü gibi serbest metinden 28 Nisan, 12 Haziran, … (tekrarlar elenir, sıralı).
     */
    public static List<LocalDate> extractDayMonthFromTurkishText(
            String text,
            LocalDate now,
            int rolloverIfOlderThanDays) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        Matcher m = TURKISH_DATE_IN_TEXT.matcher(text);
        int year = now.getYear();
        TreeSet<LocalDate> unique = new TreeSet<>();
        while (m.find()) {
            int day = Integer.parseInt(m.group(1));
            int month = monthNumberOrZero(m.group(2));
            if (month < 1) {
                continue;
            }
            try {
                LocalDate date = LocalDate.of(year, month, day);
                if (date.isBefore(now.minusDays(rolloverIfOlderThanDays))) {
                    date = date.plusYears(1);
                }
                unique.add(date);
            } catch (Exception ignored) {
            }
        }
        return new ArrayList<>(unique);
    }
}
