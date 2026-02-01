package com.asaf.whatnext.services;

import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.enums.PerformanceType;
import com.asaf.whatnext.models.*;
import com.asaf.whatnext.scraper.ScrapedEventData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    
    @Autowired
    public EventSaveService(
            ConcertEventService concertEventService,
            TheaterEventService theaterEventService,
            ExhibitionEventService exhibitionEventService,
            ArtistService artistService,
            VenueService venueService,
            CloudinaryService cloudinaryService) {
        this.concertEventService = concertEventService;
        this.theaterEventService = theaterEventService;
        this.exhibitionEventService = exhibitionEventService;
        this.artistService = artistService;
        this.venueService = venueService;
        this.cloudinaryService = cloudinaryService;
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
        
        LOGGER.info("Saved {}/{} events", savedCount, scrapedEvents.size());
        return savedCount;
    }
    
    public boolean save(ScrapedEventData data) {
        if (!data.isValid()) {
            return false;
        }
        
        Venue venue = findOrCreateVenue(data);
        String imageUrl = uploadImage(data);
        
        return switch (data.getEventType()) {
            case CONCERT -> saveConcertEvent(data, venue, imageUrl);
            case THEATER, STANDUP, PERFORMING_ART -> savePerformingArtEvent(data, venue, imageUrl);
            case EXHIBITION -> saveExhibitionEvent(data, venue, imageUrl);
            default -> false;
        };
    }
    
    private Venue findOrCreateVenue(ScrapedEventData data) {
        if (data.getVenueName() == null || data.getVenueName().isEmpty()) {
            return null;
        }
        
        Venue venue = venueService.findByName(data.getVenueName());
        if (venue == null) {
            venue = new Venue();
            venue.setName(data.getVenueName());
            venue.setLocation(data.getLocation());
            venue.setCity(ScrapedEventData.normalizeCity(data.getCity()));
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
        String venueName = venue != null ? venue.getName() : null;
        if (venueName != null && data.getStartDate() != null &&
            concertEventService.existsByTitleAndDateAndVenueAndType(
                    data.getTitle(), data.getStartDate(), venueName, EventType.CONCERT)) {
            return false;
        }
        
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
        
        if (data.getStartDate() != null &&
            theaterEventService.existsByTitleDateAndType(
                    data.getTitle(), data.getStartDate(), performanceType)) {
            return false;
        }
        
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
        event.setTitle(data.getTitle());
        event.setDescription(data.getDescription());
        event.setStartDate(data.getStartDate());
        event.setEndDate(data.getEndDate());
        event.setSource(data.getSource());
        event.setCity(ScrapedEventData.normalizeCity(data.getCity()));
        event.addTicketUrl(data.getTicketUrl());
        
        if (venue != null) {
            event.setVenue(venue);
        }
        
        if (imageUrl != null) {
            event.setImage(null, "image/jpeg", imageUrl);
        }
    }
}
