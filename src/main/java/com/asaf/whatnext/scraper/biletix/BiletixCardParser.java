package com.asaf.whatnext.scraper.biletix;

import com.asaf.whatnext.config.BiletixScraperConfig;
import com.asaf.whatnext.enums.Biletix.BiletixCategory;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.scraper.ScrapedEventData;
import com.asaf.whatnext.scraper.common.TurkishDateParser;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Biletix {@code <btx-card-item>} kartı (2026-09 Angular arayüzü). Kartta şehir yok; tarih
 * "08 Eyl 2026, 21:00" (tek), "07 Eyl 2026 - 30 Eyl 2026" (aralık) veya "08 Eyl 2026" biçiminde.
 */
public class BiletixCardParser {

    private static final Logger LOG = LoggerFactory.getLogger(BiletixCardParser.class);
    private static final Pattern DATE = Pattern.compile("(\\d{1,2})\\s+(\\p{L}{3})\\.?\\s+(\\d{4})");
    private static final Pattern TIME = Pattern.compile("(\\d{1,2}):(\\d{2})");

    public ScrapedEventData parseCard(WebElement card, BiletixCategory category) {
        String title = extractText(card, BiletixScraperConfig.EVENT_TITLE_SELECTOR);
        if (title.isEmpty()) {
            return null;
        }
        String dateStr = extractText(card, BiletixScraperConfig.EVENT_DATE_SELECTOR);
        String venueName = extractText(card, BiletixScraperConfig.EVENT_VENUE_SELECTOR);
        String ticketUrl = extractUrl(card);

        LocalDate parsedDate = parseBiletixDateString(dateStr);
        if (parsedDate == null) {
            LOG.debug("Failed to parse date: {} for event: {}", dateStr, title);
        }

        return ScrapedEventData.builder()
                .title(title)
                .rawDateString(dateStr)
                .startDate(parsedDate)
                .startTime(parseBiletixTime(dateStr))
                .endDate(parseEndDate(dateStr))
                .venueName(venueName)
                .ticketUrl(ticketUrl)
                .eventType(determineEventType(title, category))
                .source(EventSourceType.BILETIX)
                .build();
    }

    private String extractText(WebElement card, String selector) {
        try {
            return card.findElement(By.cssSelector(selector)).getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private String extractUrl(WebElement card) {
        try {
            String href = card.findElement(By.cssSelector(BiletixScraperConfig.EVENT_LINK_SELECTOR))
                    .getAttribute("href");
            return href == null ? "" : href;
        } catch (Exception e) {
            return "";
        }
    }

    /** Aralıklı kartta aralığın BAŞLANGICI (ilk tarih). */
    public LocalDate parseBiletixDateString(String dateStr) {
        return nthDate(dateStr, 0);
    }

    /** Aralıklı kartta ikinci tarih; tek tarihli kartta {@code null}. */
    public LocalDate parseEndDate(String dateStr) {
        return nthDate(dateStr, 1);
    }

    public LocalTime parseBiletixTime(String dateStr) {
        if (dateStr == null) {
            return null;
        }
        Matcher m = TIME.matcher(dateStr);
        if (!m.find()) {
            return null;
        }
        try {
            return LocalTime.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalDate nthDate(String dateStr, int n) {
        if (dateStr == null) {
            return null;
        }
        Matcher m = DATE.matcher(dateStr);
        for (int i = 0; i <= n; i++) {
            if (!m.find()) {
                return null;
            }
        }
        int month = TurkishDateParser.monthNumberOrZero(m.group(2));
        if (month == 0) {
            return null;
        }
        try {
            return LocalDate.of(Integer.parseInt(m.group(3)), month, Integer.parseInt(m.group(1)));
        } catch (Exception e) {
            return null;
        }
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
