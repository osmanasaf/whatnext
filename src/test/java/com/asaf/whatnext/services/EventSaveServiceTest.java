package com.asaf.whatnext.services;

import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.models.ConcertEvent;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.models.Venue;
import com.asaf.whatnext.repository.EventRepository;
import com.asaf.whatnext.scraper.ScrapedEventData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

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
    @Mock
    private EventRepository eventRepository;

    private EventSaveService eventSaveService;

    /**
     * Mockito'nun {@code @InjectMocks} konstrüktör enjeksiyonu primitive {@code double} parametreleri
     * (jaccard eşikleri) çözemiyor — elle inşa edilir.
     */
    @BeforeEach
    void setUp() {
        eventSaveService = new EventSaveService(
                concertEventService, theaterEventService, exhibitionEventService,
                artistService, venueService, cloudinaryService, eventRepository,
                0.5, 0.85);
    }

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

    @Test
    void save_mergesIntoExistingInsteadOfCreatingDuplicate() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        ConcertEvent existing = new ConcertEvent();
        existing.setId(1L);
        existing.setTitle("FoodieFest İstanbul");
        existing.setStartDate(date);

        when(eventRepository.findByStartDateAndCityWithVenue(date, "istanbul")).thenReturn(List.of(existing));

        ScrapedEventData data = ScrapedEventData.builder()
                .title("FoodieFest İstanbul")
                .ticketUrl("https://example.com/foodiefest-2")
                .startDate(date)
                .city("istanbul")
                .eventType(EventType.CONCERT)
                .source(EventSourceType.BUBILET)
                .build();

        boolean created = eventSaveService.save(data);

        assertThat(created).isFalse();
        ArgumentCaptor<Event> cap = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(cap.capture());
        assertThat(cap.getValue().getTicketUrls()).contains("https://example.com/foodiefest-2");
        verify(concertEventService, never()).save(any());
    }
}
