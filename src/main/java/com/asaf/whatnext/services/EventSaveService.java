package com.asaf.whatnext.services;

import com.asaf.whatnext.enums.AudienceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.enums.PerformanceType;
import com.asaf.whatnext.models.*;
import com.asaf.whatnext.repository.EventRepository;
import com.asaf.whatnext.scraper.ScrapedEventData;
import com.asaf.whatnext.utils.EventIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EventSaveService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventSaveService.class);

    private final ConcertEventService concertEventService;
    private final TheaterEventService theaterEventService;
    private final ExhibitionEventService exhibitionEventService;
    private final ArtistService artistService;
    private final VenueService venueService;
    private final CloudinaryService cloudinaryService;
    private final EventRepository eventRepository;
    private final double jaccardThreshold;
    private final double jaccardStrongThreshold;

    @Autowired
    public EventSaveService(
            ConcertEventService concertEventService,
            TheaterEventService theaterEventService,
            ExhibitionEventService exhibitionEventService,
            ArtistService artistService,
            VenueService venueService,
            CloudinaryService cloudinaryService,
            EventRepository eventRepository,
            @Value("${whatnext.dedup.title-jaccard:0.5}") double jaccardThreshold,
            @Value("${whatnext.dedup.title-jaccard-strong:0.85}") double jaccardStrongThreshold) {
        this.concertEventService = concertEventService;
        this.theaterEventService = theaterEventService;
        this.exhibitionEventService = exhibitionEventService;
        this.artistService = artistService;
        this.venueService = venueService;
        this.cloudinaryService = cloudinaryService;
        this.eventRepository = eventRepository;
        this.jaccardThreshold = jaccardThreshold;
        this.jaccardStrongThreshold = jaccardStrongThreshold;
    }
    
    public int saveAll(List<ScrapedEventData> scrapedEvents) {
        int savedCount = 0;
        
        for (ScrapedEventData data : scrapedEvents) {
            try {
                if (!data.isValid()) {
                    continue;
                }
                
                if (save(data)) {
                    savedCount++;
                }
            } catch (Exception e) {
                LOGGER.error("Error saving event: {}", data.getTitle(), e);
            }
        }
        
        int skipped = scrapedEvents.size() - savedCount;
        if (skipped > 0) {
            LOGGER.info("Saved {}/{} events ({} atlandı: mükerrer veya save false)", savedCount, scrapedEvents.size(), skipped);
        } else {
            LOGGER.info("Saved {}/{} events", savedCount, scrapedEvents.size());
        }
        return savedCount;
    }
    
    public boolean save(ScrapedEventData data) {
        if (!data.isValid()) {
            return false;
        }

        Venue venue = findOrCreateVenue(data);
        warnIfLocationSetButVenueNameMissing(data, venue);

        Event duplicate = findDuplicate(data);
        if (duplicate != null) {
            mergeIntoExisting(duplicate, data, venue);
            eventRepository.save(duplicate);
            LOGGER.debug("Duplicate merged (EventIdentity): '{}' -> existing #{} on {}",
                    data.getTitle(), duplicate.getId(), data.getStartDate());
            return false;
        }

        String imageUrl = uploadImage(data);
        return switch (data.getEventType()) {
            case CONCERT -> saveConcertEvent(data, venue, imageUrl);
            case THEATER, STANDUP, PERFORMING_ART -> savePerformingArtEvent(data, venue, imageUrl);
            case EXHIBITION -> saveExhibitionEvent(data, venue, imageUrl);
            default -> false;
        };
    }

    /**
     * Aynı gün + şehir içindeki adaylar arasında {@link EventIdentity} ile eşleşen ilk kayıt.
     * {@code startDate} yoksa (ör. detaydan tarih çıkmayan kartlar) tekilleştirme yapılmaz.
     */
    private Event findDuplicate(ScrapedEventData data) {
        if (data.getStartDate() == null) {
            return null;
        }
        String city = ScrapedEventData.normalizeCity(data.getCity());
        if (city == null || city.isEmpty()) {
            city = "istanbul";
        }
        List<Event> candidates = eventRepository.findByStartDateAndCityWithVenue(data.getStartDate(), city);
        for (Event candidate : candidates) {
            if (EventIdentity.isSameEvent(candidate, data, jaccardThreshold, jaccardStrongThreshold)) {
                return candidate;
            }
        }
        return null;
    }

    /** Atlamak yerine birleştir: ticketUrl ekle, eksik mekân/görsel/fiyat doldur (fiyatta min), CHILDREN'ı yükselt. */
    private void mergeIntoExisting(Event existing, ScrapedEventData data, Venue venue) {
        if (data.getTicketUrl() != null) {
            existing.addTicketUrl(data.getTicketUrl());
        }
        if (existing.getVenue() == null && venue != null) {
            existing.setVenue(venue);
        }
        if (existing.getEventImage() == null && data.getImageUrl() != null && !data.getImageUrl().isBlank()) {
            String imageUrl = uploadImage(data);
            if (imageUrl != null) {
                existing.setImage(null, "image/jpeg", imageUrl);
            }
        }
        if (data.getMinPriceTl() != null
                && (existing.getMinPriceTl() == null || data.getMinPriceTl() < existing.getMinPriceTl())) {
            existing.setMinPriceTl(data.getMinPriceTl());
        }
        if (existing.getStartTime() == null && data.getStartTime() != null) {
            existing.setStartTime(data.getStartTime());
        }
        if (data.getAudience() == AudienceType.CHILDREN) {
            existing.setAudience(AudienceType.CHILDREN);
        }
    }
    
    private void warnIfLocationSetButVenueNameMissing(ScrapedEventData data, Venue venue) {
        if (venue != null) {
            return;
        }
        if (data.getLocation() == null || data.getLocation().isBlank()) {
            return;
        }
        LOGGER.warn(
                "Mekan adı (venueName) yok, location dolu — scraper venueName üretmeli: source={} title='{}' location='{}'",
                data.getSource(), data.getTitle(), data.getLocation());
    }

    /** Önce slug'la (varsa) sonra adla eşler — mekân dublikasyonunu da bitirir. */
    private Venue findOrCreateVenue(ScrapedEventData data) {
        if (data.getVenueName() == null || data.getVenueName().isBlank()) {
            return null;
        }
        String name = data.getVenueName().trim();
        String slug = data.getVenueSlug();

        Venue venue = slug != null && !slug.isBlank() ? venueService.findBySlug(slug) : null;
        if (venue == null) {
            venue = venueService.findByName(name);
        }
        if (venue == null) {
            venue = new Venue();
            venue.setName(name);
            venue.setLocation(data.getLocation());
            venue.setCity(ScrapedEventData.normalizeCity(data.getCity()));
            venue.setSlug(slug);
            venue = venueService.save(venue);
        } else if (slug != null && !slug.isBlank() && (venue.getSlug() == null || venue.getSlug().isBlank())) {
            venue.setSlug(slug);
            venue = venueService.save(venue);
        }
        return venue;
    }
    
    private String uploadImage(ScrapedEventData data) {
        if (data.getImageUrl() == null || data.getImageUrl().isEmpty()) {
            return null;
        }
        
        try {
            String folder = "events/" + data.getSource().name().toLowerCase();
            String fileName = data.getSource().name().toLowerCase() + "_" + UUID.randomUUID().toString().replace("-", "");
            Map<String, Object> result = cloudinaryService.uploadImageFromUrl(data.getImageUrl(), folder, fileName);
            return cloudinaryService.getImageUrl(result);
        } catch (Exception e) {
            LOGGER.debug("Image upload failed for: {}", data.getTitle());
            return null;
        }
    }
    
    private boolean saveConcertEvent(ScrapedEventData data, Venue venue, String imageUrl) {
        ConcertEvent event = new ConcertEvent();
        setCommonFields(event, data, venue, imageUrl);
        event.setType(EventType.CONCERT);
        
        if (data.getArtistNames() != null && !data.getArtistNames().isEmpty()) {
            Artist artist = artistService.findByName(data.getArtistNames());
            if (artist == null) {
                artist = new Artist();
                artist.setName(data.getArtistNames());
                artist = artistService.save(artist);
            }
            event.setArtist(artist);
        }
        
        concertEventService.save(event);
        return true;
    }
    
    private boolean savePerformingArtEvent(ScrapedEventData data, Venue venue, String imageUrl) {
        PerformanceType performanceType = switch (data.getEventType()) {
            case STANDUP -> PerformanceType.STANDUP;
            case THEATER -> PerformanceType.THEATER;
            default -> PerformanceType.THEATER;
        };

        PerformingArt event = new PerformingArt();
        setCommonFields(event, data, venue, imageUrl);
        event.setType(EventType.PERFORMING_ART);
        event.setPerformanceType(performanceType);
        
        theaterEventService.save(event);
        return true;
    }
    
    private boolean saveExhibitionEvent(ScrapedEventData data, Venue venue, String imageUrl) {
        ExhibitionEvent event = new ExhibitionEvent();
        setCommonFields(event, data, venue, imageUrl);
        event.setType(EventType.EXHIBITION);
        
        exhibitionEventService.save(event);
        return true;
    }
    
    private void setCommonFields(Event event, ScrapedEventData data, Venue venue, String imageUrl) {
        event.setTitle(data.getTitle() != null ? data.getTitle().trim() : null);
        event.setDescription(data.getDescription());
        event.setStartDate(data.getStartDate());
        event.setStartTime(data.getStartTime());
        event.setEndDate(data.getEndDate());
        event.setSource(data.getSource());
        String city = ScrapedEventData.normalizeCity(data.getCity());
        if (city == null || city.isEmpty()) {
            city = "istanbul";
        }
        event.setCity(city);
        event.addTicketUrl(data.getTicketUrl());
        if (data.getMinPriceTl() != null) {
            event.setMinPriceTl(data.getMinPriceTl());
        }
        if (data.getAudience() != null) {
            event.setAudience(data.getAudience());
        }
        
        if (venue != null) {
            event.setVenue(venue);
        }
        
        if (imageUrl != null) {
            event.setImage(null, "image/jpeg", imageUrl);
        }
    }
}
