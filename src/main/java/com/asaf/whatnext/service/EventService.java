package com.asaf.whatnext.service;

import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.enums.City;
import com.asaf.whatnext.enums.PerformanceType;
import com.asaf.whatnext.models.*;
import com.asaf.whatnext.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Locale;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final ArtistService artistService;
    private final DirectorService directorService;
    private final VenueService venueService;

    @Autowired
    public EventService(EventRepository eventRepository,
                        ArtistService artistService,
                        DirectorService directorService,
                        VenueService venueService) {
        this.eventRepository = eventRepository;
        this.artistService = artistService;
        this.directorService = directorService;
        this.venueService = venueService;
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
        return eventRepository.findByStartDateBetweenAndCity(
                java.time.LocalDate.parse(startDate),
                java.time.LocalDate.parse(endDate),
                city.name().toLowerCase(Locale.ROOT)
        );
    }

    public List<Event> findByDateRangeAndTypeAndCity(String startDate, String endDate, EventType type, City city) {
        boolean isPerformanceType = false;
        for (PerformanceType pt : PerformanceType.values()) {
            if (pt.name().equals(type.name())) {
                isPerformanceType = true;
                break;
            }
        }
        if (type != null && isPerformanceType) {
            List<Event> events = eventRepository.findByStartDateBetweenAndTypeAndCity(
                java.time.LocalDate.parse(startDate),
                java.time.LocalDate.parse(endDate),
                EventType.PERFORMING_ART,
                city.name().toLowerCase(java.util.Locale.ROOT)
            );
            return events.stream()
                .filter(event -> event instanceof com.asaf.whatnext.models.PerformingArt performingArt &&
                        performingArt.getPerformanceType() != null &&
                        performingArt.getPerformanceType().name().equals(type.name()))
                .toList();
        } else {
            return eventRepository.findByStartDateBetweenAndTypeAndCity(
                java.time.LocalDate.parse(startDate),
                java.time.LocalDate.parse(endDate),
                type,
                city.name().toLowerCase(java.util.Locale.ROOT)
            );
        }
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