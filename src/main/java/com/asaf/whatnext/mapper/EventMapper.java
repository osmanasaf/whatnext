package com.asaf.whatnext.mapper;

import com.asaf.whatnext.dto.EventSummaryDTO;
import com.asaf.whatnext.dto.EventDetailDTO;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.models.ConcertEvent;
import com.asaf.whatnext.models.PerformingArt;
import com.asaf.whatnext.models.ExhibitionEvent;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {
    
    public EventSummaryDTO toSummaryDTO(Event event) {
        EventSummaryDTO dto = new EventSummaryDTO();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setStartDate(event.getStartDate());
        dto.setType(event.getType());
        dto.setCity(event.getCity());
        
        if (event.getVenue() != null) {
            dto.setVenueName(event.getVenue().getName());
        }
        
        if (event.getEventImage() != null) {
            dto.setImageUrl(event.getEventImage().getImageSourceUrl());
        }
        
        return dto;
    }
    
    public EventDetailDTO toDetailDTO(Event event) {
        EventDetailDTO dto = new EventDetailDTO();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setStartDate(event.getStartDate());
        dto.setEndDate(event.getEndDate());
        dto.setType(event.getType());
        dto.setSource(event.getSource());
        dto.setCity(event.getCity());
        dto.setTicketUrls(event.getTicketUrls());
        
        if (event.getVenue() != null) {
            dto.setVenueId(event.getVenue().getId());
            dto.setVenueName(event.getVenue().getName());
            dto.setVenueLocation(event.getVenue().getLocation());
        }
        
        if (event.getEventImage() != null) {
            dto.setImageUrl(event.getEventImage().getImageSourceUrl());
            dto.setImageSourceUrl(event.getEventImage().getImageSourceUrl());
        }
        
        // Event tipine göre ilişkili entity ID'lerini set et
        if (event instanceof ConcertEvent) {
            ConcertEvent concertEvent = (ConcertEvent) event;
            if (concertEvent.getArtist() != null) {
                dto.setArtistId(concertEvent.getArtist().getId());
            }
        } else if (event instanceof PerformingArt) {
            PerformingArt performingArt = (PerformingArt) event;
            if (performingArt.getDirector() != null) {
                dto.setDirectorId(performingArt.getDirector().getId());
            }
        } else if (event instanceof ExhibitionEvent) {
            ExhibitionEvent exhibitionEvent = (ExhibitionEvent) event;
            if (exhibitionEvent.getGallery() != null) {
                dto.setGalleryId(exhibitionEvent.getGallery().getId());
            }
        }
        
        return dto;
    }
} 