package com.asaf.whatnext.services;

import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.enums.City;
import com.asaf.whatnext.enums.PerformanceType;
import com.asaf.whatnext.models.*;
import com.asaf.whatnext.repository.EventRepository;
import com.asaf.whatnext.services.scoring.EventScoringStrategy;
import com.asaf.whatnext.utils.EventDuplicateKeys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final ArtistService artistService;
    private final DirectorService directorService;
    private final VenueService venueService;
    private final EventScoringStrategy scoringStrategy;

    @Autowired
    public EventService(EventRepository eventRepository,
                        ArtistService artistService,
                        DirectorService directorService,
                        VenueService venueService,
                        EventScoringStrategy scoringStrategy) {
        this.eventRepository = eventRepository;
        this.artistService = artistService;
        this.directorService = directorService;
        this.venueService = venueService;
        this.scoringStrategy = scoringStrategy;
    }

    public List<Event> findAll() {
        return eventRepository.findAll();
    }

    public Optional<Event> findById(Long id) {
        return eventRepository.findById(id);
    }

    public List<Event> findByType(EventType type) {
        return eventRepository.findByType(type);
    }

    public List<Event> findBySource(EventSourceType source) {
        return eventRepository.findBySource(source);
    }

    public List<Event> findByVenue(Venue venue) {
        return eventRepository.findByVenue(venue);
    }

    public List<Event> findByDate(String date) {
        return eventRepository.findByStartDate(java.time.LocalDate.parse(date));
    }

    public List<Event> findByCity(City city) {
        return eventRepository.findByCity(city.name().toLowerCase(Locale.ROOT));
    }

    public List<Event> findByDateRange(String startDate, String endDate) {
        return eventRepository.findByStartDateBetween(
                java.time.LocalDate.parse(startDate),
                java.time.LocalDate.parse(endDate)
        );
    }

    public List<Event> findByDateRangeAndType(String startDate, String endDate, EventType type) {
        boolean isPerformanceType = false;
        for (PerformanceType pt : PerformanceType.values()) {
            if (pt.name().equals(type.name())) {
                isPerformanceType = true;
                break;
            }
        }
        if (type != null && isPerformanceType) {
            List<Event> events = eventRepository.findByStartDateBetweenAndType(
                java.time.LocalDate.parse(startDate),
                java.time.LocalDate.parse(endDate),
                EventType.PERFORMING_ART
            );
            return events.stream()
                .filter(event -> event instanceof com.asaf.whatnext.models.PerformingArt performingArt &&
                        performingArt.getPerformanceType() != null &&
                        performingArt.getPerformanceType().name().equals(type.name()))
                .toList();
        } else {
            return eventRepository.findByStartDateBetweenAndType(
                java.time.LocalDate.parse(startDate),
                java.time.LocalDate.parse(endDate),
                type
            );
        }
    }

    public List<Event> findByDateRangeAndCity(String startDate, String endDate, City city) {
        return eventRepository.findByStartDateBetweenAndCityIgnoreCase(
                java.time.LocalDate.parse(startDate),
                java.time.LocalDate.parse(endDate),
                city.name().toLowerCase(Locale.ROOT)
        );
    }

    public List<Event> findByDateRangeAndTypeAndCity(String startDate, String endDate, EventType type, City city) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        String cityStr = city.name().toLowerCase(Locale.ROOT);
        boolean useIstanbulVariant = "istanbul".equals(cityStr);

        boolean isPerformanceType = false;
        for (PerformanceType pt : PerformanceType.values()) {
            if (pt.name().equals(type.name())) {
                isPerformanceType = true;
                break;
            }
        }

        List<Event> events;
        if (useIstanbulVariant) {
            if (type != null && isPerformanceType) {
                events = eventRepository.findByStartDateBetweenAndTypeAndIstanbul(start, end, EventType.PERFORMING_ART);
                return events.stream()
                    .filter(event -> event instanceof com.asaf.whatnext.models.PerformingArt performingArt &&
                            performingArt.getPerformanceType() != null &&
                            performingArt.getPerformanceType().name().equals(type.name()))
                    .toList();
            } else {
                events = eventRepository.findByStartDateBetweenAndTypeAndIstanbul(start, end, type);
            }
        } else {
            if (type != null && isPerformanceType) {
                events = eventRepository.findByStartDateBetweenAndTypeAndCityIgnoreCase(
                    start, end, EventType.PERFORMING_ART, cityStr);
                return events.stream()
                    .filter(event -> event instanceof com.asaf.whatnext.models.PerformingArt performingArt &&
                            performingArt.getPerformanceType() != null &&
                            performingArt.getPerformanceType().name().equals(type.name()))
                    .toList();
            } else {
                events = eventRepository.findByStartDateBetweenAndTypeAndCityIgnoreCase(start, end, type, cityStr);
            }
        }
        return events;
    }

    /**
     * O gün aktif etkinliklerden skoru en yüksek {@code limit} tanesi. Sıralama limitten ÖNCE yapılır —
     * önceden sorgu {@code startDate, id} sırasıyla kesiliyordu, yani "ilk kazınan N etkinlik" seçiliyor,
     * popüler ama sonradan kazınmış etkinlikler hiç havuza girmiyordu. THEATER/STANDUP doğrudan verilebilir.
     * Skor LAZY ilişkilere (venue, artist, actors) dokunur: çağıran açık bir transaction içinde olmalı.
     */
    public List<Event> findTopNEventsForDateAndCity(LocalDate date, EventType eventType, String city, int limit) {
        String cityStr = city.toLowerCase(Locale.ROOT);
        boolean isPerformanceType = false;
        for (PerformanceType pt : PerformanceType.values()) {
            if (pt.name().equals(eventType.name())) {
                isPerformanceType = true;
                break;
            }
        }
        EventType queryType = isPerformanceType ? EventType.PERFORMING_ART : eventType;
        List<Event> events = "istanbul".equals(cityStr)
                ? eventRepository.findByTypeAndIstanbulActiveOnDate(queryType, date)
                : eventRepository.findByTypeAndCityActiveOnDate(queryType, cityStr, date);
        boolean filterPerformance = isPerformanceType;
        List<Event> sorted = events.stream()
                .filter(e -> !filterPerformance || (e instanceof PerformingArt pa
                        && pa.getPerformanceType() != null
                        && pa.getPerformanceType().name().equals(eventType.name())))
                .filter(e -> e.getTitle() != null && !e.getTitle().isBlank())
                .sorted(Comparator.comparingInt(scoringStrategy::calculateScore).reversed())
                .collect(Collectors.toList());
        return EventDuplicateKeys.dedupeByTitleAndDateKeepOrder(sorted).stream().limit(limit).collect(Collectors.toList());
    }

    @Transactional
    public Event save(Event event) {
        if (event instanceof ConcertEvent) {
            ConcertEvent concertEvent = (ConcertEvent) event;
            if (concertEvent.getArtist() != null) {
                Artist savedArtist = artistService.save(concertEvent.getArtist());
                concertEvent.setArtist(savedArtist);
            }
        } else if (event instanceof PerformingArt) {
            PerformingArt performingArt = (PerformingArt) event;
            if (performingArt.getDirector() != null) {
                Director savedDirector = directorService.save(performingArt.getDirector());
                performingArt.setDirector(savedDirector);
            }
        }

        if (event.getVenue() != null) {
            if(event.getId() == null){
                Venue savedVenue = venueService.save(event.getVenue());
                event.setVenue(savedVenue);
            }
        }

        return eventRepository.save(event);
    }

    public void deleteById(Long id) {
        eventRepository.deleteById(id);
    }
} 