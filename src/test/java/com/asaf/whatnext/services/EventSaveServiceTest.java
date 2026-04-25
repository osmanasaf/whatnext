package com.asaf.whatnext.services;

import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.models.Venue;
import com.asaf.whatnext.scraper.ScrapedEventData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventSaveServiceTest {

    @Mock
    private ConcertEventService concertEventService;
    @Mock
    private TheaterEventService theaterEventService;
    @Mock
    private ExhibitionEventService exhibitionEventService;
    @Mock
    private ArtistService artistService;
    @Mock
    private VenueService venueService;
    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private EventSaveService eventSaveService;

    @Test
    void save_doesNotCreateVenueFromLocationAlone() {
        ScrapedEventData data = ScrapedEventData.builder()
                .title("Konser X")
                .ticketUrl("https://example.com/t")
                .location("Kadıköy, İstanbul")
                .venueName(null)
                .eventType(EventType.CONCERT)
                .source(EventSourceType.BILETINO)
                .build();

        eventSaveService.save(data);

        verify(venueService, never()).findByName(anyString());
        verify(venueService, never()).save(any(Venue.class));
    }

    @Test
    void save_createsVenueWhenVenueNamePresent() {
        ScrapedEventData data = ScrapedEventData.builder()
                .title("Konser Y")
                .ticketUrl("https://example.com/y")
                .location("Adres")
                .venueName("  Zorlu PSM  ")
                .city("İstanbul")
                .eventType(EventType.CONCERT)
                .source(EventSourceType.BUBILET)
                .build();

        when(venueService.findByName("Zorlu PSM")).thenReturn(null);
        when(venueService.save(any(Venue.class))).thenAnswer(inv -> inv.getArgument(0));
        eventSaveService.save(data);

        verify(venueService).findByName("Zorlu PSM");
        ArgumentCaptor<Venue> cap = ArgumentCaptor.forClass(Venue.class);
        verify(venueService).save(cap.capture());
        assertThat(cap.getValue().getName()).isEqualTo("Zorlu PSM");
    }
}
