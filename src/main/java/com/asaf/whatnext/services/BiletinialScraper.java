package com.asaf.whatnext.services;

import com.asaf.whatnext.enums.Biletinial.BiletinialCategory;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.enums.PerformanceType;
import com.asaf.whatnext.models.*;
import com.asaf.whatnext.service.ArtistService;
import com.asaf.whatnext.service.ConcertEventService;
import com.asaf.whatnext.service.TheaterEventService;
import com.asaf.whatnext.service.VenueService;
import com.asaf.whatnext.utils.WebScraperUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class BiletinialScraper implements EventSource {
    private static final Logger LOGGER = Logger.getLogger(BiletinialScraper.class.getName());
    private static final String CHROME_PATH = "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";
    private static final int WAIT_TIMEOUT = 60;
    private static final String BASE_URL = "https://biletinial.com";
    private static final String SEARCH_URL_TEMPLATE = "%s/tr-tr/%s/istanbul?date=%s&filmtypeid=0&loc=0&thisweekend=";
    private static final String EVENT_CARD_SELECTOR = "#kategori__etkinlikler li";
    private static final String TITLE_SELECTOR = "h3 a";
    private static final String DATE_SELECTOR = "span";
    private static final String LOCATION_SELECTOR = "address";
    private static final String HREF_ATTRIBUTE = "href";
    private static final String SMALL_TAG = "<small>";
    private static final String SMALL_TAG_CLOSE = "</small>";
    private static final String DATE_SEPARATOR = " - ";

    private final ConcertEventService concertEventService;
    private final TheaterEventService theaterEventService;
    private final ArtistService artistService;
    private final VenueService venueService;
    private WebDriver driver;
    private WebDriverWait wait;

    @Autowired
    public BiletinialScraper(ConcertEventService concertEventService,
                           TheaterEventService theaterEventService,
                           ArtistService artistService,
                           VenueService venueService) {
        this.concertEventService = concertEventService;
        this.theaterEventService = theaterEventService;
        this.artistService = artistService;
        this.venueService = venueService;
    }

    private void initializeBrowser() {
        this.driver = WebScraperUtils.initializeBrowser(CHROME_PATH, WAIT_TIMEOUT);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIMEOUT));
    }

    private void closeBrowser() {
        if (driver != null) {
            WebScraperUtils.closeBrowser(driver);
        }
    }

    @Override
    public List<Event> fetchEvents() {
        List<Event> allEvents = new ArrayList<>();
        try {
            allEvents.addAll(processCategory(BiletinialCategory.THEATRE));
            allEvents.addAll(processCategory(BiletinialCategory.MUSIC));
            allEvents.addAll(processCategory(BiletinialCategory.STANDUP));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching events from Biletinial", e);
        }
        return allEvents;
    }

    public List<Event> processCategory(BiletinialCategory category) {
        List<Event> events = new ArrayList<>();
        try {
            initializeBrowser();
            LOGGER.info("Processing category: " + category.name());
            
            LocalDate today = LocalDate.now();
            for (int i = 0; i < 7; i++) {
                LocalDate date = today.plusDays(i);
                String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                String searchUrl = buildSearchUrl(category, dateStr);
                
                LOGGER.info("Fetching events for date: " + dateStr);
                List<Event> dateEvents = fetchEventsByDate(searchUrl, category);
                events.addAll(dateEvents);
            }
            
            LOGGER.info("Processed " + events.size() + " events for category: " + category.name());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing category: " + category.name(), e);
        } finally {
            closeBrowser();
        }
        return events;
    }

    private String buildSearchUrl(BiletinialCategory category, String date) {
        return String.format(SEARCH_URL_TEMPLATE, BASE_URL, category.getValue(), date);
    }

    private List<Event> fetchEventsByDate(String searchUrl, BiletinialCategory category) {
        List<Event> events = new ArrayList<>();
        try {
            WebScraperUtils.navigateToUrl(driver, wait, searchUrl);
            List<WebElement> eventCards = driver.findElements(By.cssSelector(EVENT_CARD_SELECTOR));
            
            for (WebElement card : eventCards) {
                try {
                    Event event = extractEventFromCard(card, category);
                    if (event != null) {
                        events.add(event);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error processing event card", e);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching events for URL: " + searchUrl, e);
        }
        return events;
    }

    private Event extractEventFromCard(WebElement card, BiletinialCategory category) {
        try {
            String title = card.findElement(By.cssSelector(TITLE_SELECTOR)).getText();
            String dateStr = card.findElement(By.cssSelector(DATE_SELECTOR)).getText();
            String location = card.findElement(By.cssSelector(LOCATION_SELECTOR)).getText();
            String ticketUrl = card.findElement(By.cssSelector(TITLE_SELECTOR)).getAttribute(HREF_ATTRIBUTE);
            
            if (title.isEmpty() || dateStr.isEmpty()) {
                return null;
            }

            EventType eventType = determineEventType(title, category);
            return createEvent(eventType, title, dateStr, location, ticketUrl);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error extracting event from card", e);
            return null;
        }
    }

    private EventType determineEventType(String title, BiletinialCategory category) {
        return switch (category) {
            case THEATRE -> title.toLowerCase().contains("stand-up") ? EventType.STANDUP : EventType.THEATER;
            case MUSIC -> EventType.CONCERT;
            case STANDUP -> EventType.STANDUP;
            default -> EventType.THEATER;
        };
    }

    private Event createEvent(EventType eventType, String title, String dateStr, String location, String ticketUrl) {
        Event event = switch (eventType) {
            case CONCERT -> new ConcertEvent();
            case THEATER, STANDUP -> {
                TheaterEvent theaterEvent = new TheaterEvent();
                theaterEvent.setPerformanceType(eventType == EventType.STANDUP ? PerformanceType.STANDUP : PerformanceType.THEATER);
                yield theaterEvent;
            }
            default -> null;
        };

        if (event == null) {
            return null;
        }

        event.setTitle(title);
        event.setStartDate(parseDate(dateStr));
        event.setSource(EventSourceType.BILETINIAL);
        event.setType(eventType);
        event.addTicketUrl(ticketUrl);

        String[] locationParts = location.split(SMALL_TAG);
        if (locationParts.length >= 2) {
            String venueName = locationParts[1].replace(SMALL_TAG_CLOSE, "").trim();
            event.setVenue(venueService.findOrCreateVenue(venueName));
        }

        return event;
    }

    private LocalDate parseDate(String dateStr) {
        try {
            String[] parts = dateStr.split(DATE_SEPARATOR);
            if (parts.length >= 2) {
                String month = parts[0].trim();
                int day = Integer.parseInt(parts[1].trim());
                int year = LocalDate.now().getYear();
                int monthNum = getMonthNumber(month);
                return LocalDate.of(year, monthNum, day);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error parsing date: " + dateStr, e);
        }
        return null;
    }

    private int getMonthNumber(String month) {
        Map<String, Integer> months = new HashMap<>();
        months.put("Ocak", 1);
        months.put("Şubat", 2);
        months.put("Mart", 3);
        months.put("Nisan", 4);
        months.put("Mayıs", 5);
        months.put("Haziran", 6);
        months.put("Temmuz", 7);
        months.put("Ağustos", 8);
        months.put("Eylül", 9);
        months.put("Ekim", 10);
        months.put("Kasım", 11);
        months.put("Aralık", 12);
        return months.getOrDefault(month, 1);
    }

    @Override
    public String getSourceName() {
        return "Biletinial";
    }

    @Override
    public boolean isAvailable() {
        try {
            driver = WebScraperUtils.initializeBrowser(CHROME_PATH, WAIT_TIMEOUT);
            driver.get(BASE_URL);
            boolean available = driver.getTitle().contains("Biletinial");
            WebScraperUtils.closeBrowser(driver);
            return available;
        } catch (Exception e) {
            if (driver != null) {
                WebScraperUtils.closeBrowser(driver);
            }
            return false;
        }
    }

    @Override
    public String getSourceUrl() {
        return BASE_URL;
    }
} 