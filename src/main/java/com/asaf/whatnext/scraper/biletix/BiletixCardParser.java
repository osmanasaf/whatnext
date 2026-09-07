package com.asaf.whatnext.scraper.biletix;

import com.asaf.whatnext.config.BiletixScraperConfig;
import com.asaf.whatnext.enums.Biletix.BiletixCategory;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.scraper.ScrapedEventData;
import com.asaf.whatnext.scraper.common.ElementTextExtractor;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BiletixCardParser {

    private static final Logger LOG = LoggerFactory.getLogger(BiletixCardParser.class);

    public ScrapedEventData parseCard(WebElement card, BiletixCategory category) {
        String title = extractText(card, BiletixScraperConfig.EVENT_TITLE_SELECTOR);
        if (title.isEmpty()) {
            title = extractText(card, BiletixScraperConfig.EVENT_TITLE_MOBILE_SELECTOR);
        }

        if (title.isEmpty()) {
            return null;
        }

        String dateStr = extractDate(card);
        if (dateStr.isEmpty()) {
            return null;
        }

        String venueName = ElementTextExtractor.firstNonBlank(
                sel -> extractText(card, sel),
                BiletixScraperConfig.EVENT_VENUE_ALTERNATE_SELECTORS);
        String location = extractText(card, BiletixScraperConfig.EVENT_LOCATION_SELECTOR);
        String ticketUrl = extractUrl(card);

        EventType eventType = determineEventType(title, category);
        LocalDate parsedDate = parseBiletixDateString(dateStr);

        if (parsedDate == null) {
            LOG.debug("Failed to parse date: {} for event: {}", dateStr, title);
        }

        return ScrapedEventData.builder()
                .title(title)
                .rawDateString(dateStr)
                .startDate(parsedDate)
                .venueName(venueName)
                .location(location)
                .city(ScrapedEventData.normalizeCity(location))
                .ticketUrl(ticketUrl)
                .eventType(eventType)
                .source(EventSourceType.BILETIX)
                .build();
    }

    private String extractText(WebElement card, String selector) {
        try {
            return card.findElement(By.cssSelector(selector)).getText();
        } catch (Exception e) {
            return "";
        }
    }

    private String extractDate(WebElement card) {
        try {
            String firstDate = card.findElement(
                    By.cssSelector(BiletixScraperConfig.EVENT_DATE_SELECTOR)).getText();
            String secondDate = card.findElement(
                    By.cssSelector(BiletixScraperConfig.EVENT_SECONDARY_DATE_SELECTOR)).getText();
            // .fld3 .ln1 tek tarihli kartlarda etkinlik tarihi, aralıklı/tekrarlayan kartlarda
            // (turne, rezidans, "X Etkinlikleri" mekan-agregatörü) aralığın BAŞLANGICI; .ln2 ise o
            // durumda aralığın bitişi (bazen aylar sonrası) — canlı doğrulamada ln1 hep dolu
            // olduğundan başlangıç tarihi önceliklidir.
            return !firstDate.isEmpty() ? firstDate : secondDate;
        } catch (Exception e) {
            try {
                String day = card.findElement(By.cssSelector(".searchMobileDateDayNumber")).getText();
                String month = card.findElement(By.cssSelector(".searchMobileDateMonth")).getText();
                String year = card.findElement(By.cssSelector(".searchMobileDateYear")).getText();
                return day + "/" + month + "/" + year;
            } catch (Exception ex) {
                return "";
            }
        }
    }

    private String extractUrl(WebElement card) {
        try {
            return card.findElement(By.cssSelector(BiletixScraperConfig.EVENT_TITLE_SELECTOR))
                    .getAttribute("href");
        } catch (Exception e) {
            try {
                return card.findElement(By.cssSelector(BiletixScraperConfig.EVENT_LINK_SELECTOR))
                        .getAttribute("href");
            } catch (Exception ex) {
                return "";
            }
        }
    }

    public LocalDate parseBiletixDateString(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        String normalized = dateStr.trim();
        int commaIndex = normalized.indexOf(',');
        if (commaIndex >= 0) {
            normalized = normalized.substring(commaIndex + 1).trim();
        }
        normalized = normalized.replaceAll("\\s+", "");
        // Aralıklı kartlarda .ln1 sonda "-" taşır (örn. "13/07/26-"); parse öncesi kırpılmalı.
        normalized = normalized.replaceAll("-+$", "");

        List<String> patterns = Arrays.asList("dd/MM/yy", "dd/MM/yyyy");
        for (String pattern : patterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                return LocalDate.parse(normalized, formatter);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private EventType determineEventType(String title, BiletixCategory category) {
        if (category == BiletixCategory.MUSIC) {
            return EventType.CONCERT;
        }

        String lowerTitle = title.toLowerCase(Locale.forLanguageTag("tr"));
        if (lowerTitle.contains("stand") || lowerTitle.contains("komedi")) {
            return EventType.STANDUP;
        }

        return EventType.THEATER;
    }
}
