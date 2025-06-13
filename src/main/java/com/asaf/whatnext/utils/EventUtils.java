package com.asaf.whatnext.utils;

import com.asaf.whatnext.enums.Biletino.BiletinoCategory;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.enums.PerformanceType;
import com.asaf.whatnext.models.Artist;
import com.asaf.whatnext.models.ConcertEvent;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.models.ExhibitionEvent;
import com.asaf.whatnext.models.PerformingArt;
import com.asaf.whatnext.models.Venue;
import com.asaf.whatnext.service.ArtistService;
import com.asaf.whatnext.service.ConcertEventService;
import com.asaf.whatnext.service.ExhibitionEventService;
import com.asaf.whatnext.service.TheaterEventService;
import com.asaf.whatnext.service.VenueService;
import com.asaf.whatnext.services.DetailedEventInfo;
import com.asaf.whatnext.services.EventInfo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EventUtils {
    private static final Logger LOGGER = Logger.getLogger(EventUtils.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter SEARCH_RESULT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy");

    public static void setCommonEventProperties(Event event, String title, String description, String dateStr, String ticketUrl) {
        event.setTitle(title);
        event.setDescription(description);

        if (dateStr != null && !dateStr.isEmpty()) {
            event.setStartDate(parseDate(dateStr));
        }

        event.setSource(EventSourceType.BILETINO);

        if (ticketUrl != null && !ticketUrl.isEmpty()) {
            event.addTicketUrl(ticketUrl);
        }
    }

    public static void setVenueProperties(ConcertEvent event, String venueName, String location) {
        if ((venueName != null && !venueName.isEmpty()) || (location != null && !location.isEmpty())) {
            Venue venue = new Venue();
            venue.setName(venueName);
            venue.setLocation(location);
            event.setVenue(venue);
        }
    }

    public static ConcertEvent createConcertEvent(EventInfo info, EventType eventType) {
        ConcertEvent concert = new ConcertEvent();
        setCommonEventProperties(concert, info.getTitle(), "", info.getDateStr(), info.getTicketUrl());
        concert.setType(eventType);
        Artist artist = new Artist();
        artist.setName(extractArtistName(info.getTitle(), ""));
        concert.setArtist(artist);
        setVenueProperties(concert, info.getVenueName(), info.getLocation());
        return concert;
    }

    public static PerformingArt createTheaterEvent(EventInfo info, EventType eventType) {
        PerformingArt theater = new PerformingArt();
        setCommonEventProperties(theater, info.getTitle(), "", info.getDateStr(), info.getTicketUrl());
        theater.setType(eventType);
        if (eventType == EventType.STANDUP) {
            theater.setPerformanceType(PerformanceType.STANDUP);
        } else {
            theater.setPerformanceType(PerformanceType.THEATER);
        }
        return theater;
    }

    public static ExhibitionEvent createExhibitionEvent(EventInfo info, EventType eventType) {
        ExhibitionEvent exhibition = new ExhibitionEvent();
        setCommonEventProperties(exhibition, info.getTitle(), "", info.getDateStr(), info.getTicketUrl());
        exhibition.setType(eventType);
        return exhibition;
    }

    public static ConcertEvent createDetailedConcertEvent(DetailedEventInfo info, EventType eventType) {
        ConcertEvent concert = new ConcertEvent();
        setCommonEventProperties(concert, info.getTitle(), info.getDescription(), info.getDateStr(), info.getTicketUrl());
        concert.setType(eventType);
        Artist artist = new Artist();
        artist.setName(extractArtistName(info.getTitle(), info.getDescription()));
        concert.setArtist(artist);
        setVenueProperties(concert, info.getVenueName(), info.getLocation());
        return concert;
    }

    public static PerformingArt createDetailedTheaterEvent(DetailedEventInfo info, EventType eventType) {
        PerformingArt theater = new PerformingArt();
        setCommonEventProperties(theater, info.getTitle(), info.getDescription(), info.getDateStr(), info.getTicketUrl());
        theater.setType(eventType);
        if (eventType == EventType.STANDUP) {
            theater.setPerformanceType(PerformanceType.STANDUP);
        } else {
            theater.setPerformanceType(PerformanceType.THEATER);
        }
        return theater;
    }

    public static ExhibitionEvent createDetailedExhibitionEvent(DetailedEventInfo info, EventType eventType) {
        ExhibitionEvent exhibition = new ExhibitionEvent();
        setCommonEventProperties(exhibition, info.getTitle(), info.getDescription(), info.getDateStr(), info.getTicketUrl());
        exhibition.setType(eventType);
        return exhibition;
    }

    public static EventType determineEventType(String title, String description, EventType defaultType) {
        String lowerTitle = title.toLowerCase();
        String lowerDesc = description.toLowerCase();

        if (lowerTitle.contains("stand") || lowerDesc.contains("stand up") ||
                lowerTitle.contains("stand-up") || lowerDesc.contains("stand-up")) {
            return EventType.STANDUP;
        } else if (lowerTitle.contains("konser") || lowerDesc.contains("konser")) {
            return EventType.CONCERT;
        } else if (lowerTitle.contains("tiyatro") || lowerDesc.contains("tiyatro")) {
            return EventType.THEATER;
        } else if (lowerTitle.contains("sergi") || lowerDesc.contains("sergi")) {
            return EventType.EXHIBITION;
        }
        return defaultType;
    }

    public static EventType categoryToEventType(BiletinoCategory category) {
        switch (category) {
            case MUSIC:    return EventType.CONCERT;
            case THEATRE:  return EventType.THEATER;
            case COMEDY:   return EventType.STANDUP;
            default:       return EventType.EXHIBITION;
        }
    }


    public static String extractArtistName(String title, String description) {
        if (title.contains("-")) {
            return title.split("-")[0].trim();
        }
        return title.trim();
    }

    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            LOGGER.warning("Error parsing date: Empty date string");
            return null;
        }

        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            try {
                if (dateStr.contains(", ")) {
                    String[] parts = dateStr.split(", ");
                    if (parts.length > 1) {
                        String datePart = parts[1].trim();
                        if (datePart.contains(" ")) {
                            datePart = datePart.substring(0, datePart.indexOf(" "));
                        }
                        return LocalDate.parse(datePart, SEARCH_RESULT_DATE_FORMATTER);
                    }
                } else if (dateStr.contains("/")) {
                    return LocalDate.parse(dateStr, SEARCH_RESULT_DATE_FORMATTER);
                }

                LOGGER.warning("Error parsing date: " + dateStr + " - Invalid format");
                return null;
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Error parsing date: " + dateStr, ex);
                return null;
            }
        }
    }

    public static String getMonthNumber(String monthName) {
        if (monthName == null || monthName.isEmpty()) {
            return "01";
        }

        String lowerMonth = monthName.toLowerCase();

        switch (lowerMonth) {
            case "oca": return "01"; // Ocak (January)
            case "şub": return "02"; // Şubat (February)
            case "mar": return "03"; // Mart (March)
            case "nis": return "04"; // Nisan (April)
            case "may": return "05"; // Mayıs (May)
            case "haz": return "06"; // Haziran (June)
            case "tem": return "07"; // Temmuz (July)
            case "ağu": return "08"; // Ağustos (August)
            case "eyl": return "09"; // Eylül (September)
            case "eki": return "10"; // Ekim (October)
            case "kas": return "11"; // Kasım (November)
            case "ara": return "12"; // Aralık (December)
            default:
                LOGGER.warning("Unknown month abbreviation: " + monthName);
                return "01"; // Default to January if unknown
        }
    }

    public static void saveConcertEventWithDuplicateChecking(ConcertEvent event, 
                                                           ConcertEventService concertEventService,
                                                           ArtistService artistService,
                                                           VenueService venueService) {
        if (event.getArtist() == null || event.getArtist().getName() == null || event.getStartDate() == null) {
            LOGGER.warning("Cannot check for duplicates: ConcertEvent missing artist name or start date");
            return;
        }

        String artistName = event.getArtist().getName();
        LocalDate startDate = event.getStartDate();

        if (!concertEventService.existsByArtistNameAndDate(artistName, startDate)) {
            LOGGER.info("Saving new concert event: " + event.getTitle() + " - " + artistName + " - " + startDate);

            Artist artist = event.getArtist();
            Artist existingArtist = artistService.findByName(artistName);
            if (existingArtist != null) {
                event.setArtist(existingArtist);
            } else {
                artist = artistService.save(artist);
                event.setArtist(artist);
            }

            if (event.getVenue() != null && event.getVenue().getName() != null) {
                Venue venue = event.getVenue();
                Venue existingVenue = venueService.findByName(venue.getName());
                if (existingVenue != null) {
                    event.setVenue(existingVenue);
                } else {
                    venue = venueService.save(venue);
                    event.setVenue(venue);
                }
            }

            concertEventService.save(event);
        } else {
            LOGGER.info("Skipping duplicate concert event: " + event.getTitle() + " - " + artistName + " - " + startDate);
        }
    }

    public static void savePerformingArtWithDuplicateChecking(PerformingArt event, TheaterEventService theaterEventService) {
        if (event.getTitle() == null || event.getStartDate() == null || event.getPerformanceType() == null) {
            LOGGER.warning("Cannot check for duplicates: PerformingArt missing title, start date, or performance type");
            return;
        }

        String title = event.getTitle();
        LocalDate startDate = event.getStartDate();
        PerformanceType performanceType = event.getPerformanceType();

        if (!theaterEventService.existsByTitleDateAndType(title, startDate, performanceType)) {
            LOGGER.info("Saving new performing art event: " + title + " - " + startDate + " - " + performanceType);
            theaterEventService.save(event);
        } else {
            LOGGER.info("Skipping duplicate performing art event: " + title + " - " + startDate + " - " + performanceType);
        }
    }

    public static void saveExhibitionEventWithDuplicateChecking(ExhibitionEvent event, ExhibitionEventService exhibitionEventService) {
        LOGGER.info("Saving exhibition event: " + event.getTitle());
        exhibitionEventService.save(event);
    }
}