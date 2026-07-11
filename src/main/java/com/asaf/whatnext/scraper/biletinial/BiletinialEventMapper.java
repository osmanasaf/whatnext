package com.asaf.whatnext.scraper.biletinial;

import com.asaf.whatnext.enums.Biletinial.BiletinialCategory;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.scraper.ScrapedEventData;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;

public final class BiletinialEventMapper {

    private BiletinialEventMapper() {
    }

    public static EventType toEventType(String title, BiletinialCategory category) {
        String lowerTitle = title.toLowerCase(Locale.forLanguageTag("tr"));
        return switch (category) {
            case THEATRE -> lowerTitle.contains("stand") || lowerTitle.contains("stand-up")
                    ? EventType.STANDUP : EventType.THEATER;
            case MUSIC -> EventType.CONCERT;
            case STANDUP -> EventType.STANDUP;
            default -> EventType.THEATER;
        };
    }

    public static ScrapedEventData fromDetailSession(
            BiletinialCardDraft d,
            LocalDate startDate,
            LocalTime startTime,
            String rawDateString,
            String venue) {
        EventType eventType = toEventType(d.title(), d.category());
        return ScrapedEventData.builder()
                .title(d.title())
                .rawDateString(rawDateString)
                .startDate(startDate)
                .startTime(startTime)
                .venueName(venue)
                .location(venue)
                .city(ScrapedEventData.normalizeCity(d.cityLabel()))
                .ticketUrl(d.ticketUrl())
                .imageUrl(d.imageUrl())
                .eventType(eventType)
                .source(EventSourceType.BILETINIAL)
                .build();
    }

    public static ScrapedEventData fromListOnly(BiletinialCardDraft d) {
        EventType eventType = toEventType(d.title(), d.category());
        return ScrapedEventData.builder()
                .title(d.title())
                .rawDateString(d.listDate().toString())
                .startDate(d.listDate())
                .venueName(d.venueName())
                .location(d.venueName())
                .city(ScrapedEventData.normalizeCity(d.cityLabel()))
                .ticketUrl(d.ticketUrl())
                .imageUrl(d.imageUrl())
                .eventType(eventType)
                .source(EventSourceType.BILETINIAL)
                .build();
    }
}
