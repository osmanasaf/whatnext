package com.asaf.whatnext.scraper.biletino;

import com.asaf.whatnext.enums.Biletino.BiletinoCategory;
import com.asaf.whatnext.enums.Biletino.BiletinoCity;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.scraper.ScrapedEventData;
import org.jsoup.nodes.Element;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.asaf.whatnext.config.BiletinoScraperConfig.BASE_URL;
import static com.asaf.whatnext.config.BiletinoScraperConfig.EVENT_DATE_SELECTOR;
import static com.asaf.whatnext.config.BiletinoScraperConfig.EVENT_LINK_SELECTOR;
import static com.asaf.whatnext.config.BiletinoScraperConfig.EVENT_LOCATION_SELECTOR;
import static com.asaf.whatnext.config.BiletinoScraperConfig.EVENT_TITLE_SELECTOR;

public class BiletinoCardParser {

    private static final Logger LOG = LoggerFactory.getLogger(BiletinoCardParser.class);

    public ScrapedEventData parseCard(Element card, BiletinoCategory category, BiletinoCity searchCity) {
        String title = card.select(EVENT_TITLE_SELECTOR).text();
        String dateStr = card.select(EVENT_DATE_SELECTOR).text();
        String location = card.select(EVENT_LOCATION_SELECTOR).text();
        String ticketUrl = BASE_URL + card.select(EVENT_LINK_SELECTOR).attr("href");
        String imageUrl = card.select(".event-image img").attr("src");

        if (title.isEmpty() || ticketUrl.equals(BASE_URL)) {
            return null;
        }

        BiletinoLocationParser.VenueCity loc = BiletinoLocationParser.parse(location, searchCity);

        LocalDate parsedDate = parseEnglishMonthDate(dateStr);
        if (parsedDate == null) {
            LOG.debug("Failed to parse date: {} for event: {}", dateStr, title);
        }

        return ScrapedEventData.builder()
                .title(title)
                .rawDateString(dateStr)
                .startDate(parsedDate)
                .venueName(loc.venueName())
                .location(location)
                .city(ScrapedEventData.normalizeCity(loc.city()))
                .ticketUrl(ticketUrl)
                .imageUrl(imageUrl)
                .eventType(categoryToEventType(category))
                .source(EventSourceType.BILETINO)
                .build();
    }

    private LocalDate parseEnglishMonthDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        try {
            String startPart = dateStr.contains("-") ? dateStr.split("-")[0].trim() : dateStr.trim();
            String[] tokens = startPart.split("\s+");

            if (tokens.length >= 2) {
                String day = tokens[0];
                String month = tokens[1];
                int currentYear = com.asaf.whatnext.utils.TrClock.today().getYear();

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);
                return LocalDate.parse(String.format("%s %s %d", day, month, currentYear), formatter);
            }
        } catch (DateTimeParseException e) {
            LOG.debug("Could not parse date: {}", dateStr);
        }

        return null;
    }

    private EventType categoryToEventType(BiletinoCategory category) {
        return switch (category) {
            case MUSIC -> EventType.CONCERT;
            case THEATRE -> EventType.THEATER;
            case COMEDY -> EventType.STANDUP;
            default -> EventType.CONCERT;
        };
    }
}
