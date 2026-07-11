package com.asaf.whatnext.scraper.bubilet;

import com.asaf.whatnext.enums.Bubilet.BubiletCategory;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.scraper.ScrapedEventData;
import com.asaf.whatnext.scraper.common.ScrapedEventFactory;
import com.asaf.whatnext.scraper.common.TurkishDateParser;

import java.time.LocalDate;

/**
 * {@link BubiletCardDraft} → {@link ScrapedEventData} eşlemesi.
 */
public final class BubiletEventMapper {

    private BubiletEventMapper() {
    }

    public static ScrapedEventData toScraped(
            BubiletCardDraft d,
            LocalDate startDate,
            String rawDate,
            EventType eventType) {
        String venue = d.venueName() != null && !d.venueName().isBlank() ? d.venueName().trim() : null;
        var b = ScrapedEventFactory.baseFor(EventSourceType.BUBILET)
                .title(d.title())
                .rawDateString(rawDate)
                .startDate(startDate)
                .ticketUrl(d.ticketUrl())
                .imageUrl(d.imageUrl())
                .eventType(eventType)
                .city(ScrapedEventData.normalizeCity(d.city()))
                .minPriceTl(d.minPriceTl())
                .venueSlug(d.venueSlug());
        if (venue != null) {
            b.venueName(venue).location(venue);
        }
        return b.build();
    }

    public static EventType toEventType(BubiletCategory category) {
        return switch (category) {
            case THEATRE -> EventType.THEATER;
            case MUSIC -> EventType.CONCERT;
            case STANDUP -> EventType.STANDUP;
        };
    }

    public static LocalDate parseListDateLine(String dateText, int rolloverIfOlderThanDays) {
        return TurkishDateParser.parseBubiletListDate(dateText, com.asaf.whatnext.utils.TrClock.today(), rolloverIfOlderThanDays);
    }
}
