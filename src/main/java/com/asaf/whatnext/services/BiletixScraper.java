package com.asaf.whatnext.services;

import com.asaf.whatnext.config.BiletixScraperConfig;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.enums.PerformanceType;
import com.asaf.whatnext.models.*;
import com.asaf.whatnext.service.ArtistService;
import com.asaf.whatnext.service.ConcertEventService;
import com.asaf.whatnext.service.TheaterEventService;
import com.asaf.whatnext.service.ExhibitionEventService;
import com.asaf.whatnext.service.VenueService;
import com.asaf.whatnext.utils.EventUtils;
import com.asaf.whatnext.utils.WebScraperUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.asaf.whatnext.enums.Biletix.BiletixCategory;

import static com.asaf.whatnext.config.BiletixScraperConfig.*;

@Service
public class BiletixScraper implements EventSource {

    private static final Logger LOGGER = Logger.getLogger(BiletixScraper.class.getName());

    private final ConcertEventService concertEventService;
    private final TheaterEventService theaterEventService;
    private final ExhibitionEventService exhibitionEventService;
    private final ArtistService artistService;
    private final VenueService venueService;
    private final BiletixScraperConfig config;
    private final BiletixEventExtractor eventExtractor;

    private WebDriver driver;
    private WebDriverWait wait;

    /**
     * Initializes the BiletixScraper with a configured WebDriver and required services.
     * Sets up Chrome with appropriate options for web scraping.
     * 
     * @param concertEventService Service for managing concert events
     * @param theaterEventService Service for managing theater events
     * @param exhibitionEventService Service for managing exhibition events
     * @param artistService Service for managing artist entities
     * @param venueService Service for managing venue entities
     */
    @Autowired
    public BiletixScraper(ConcertEventService concertEventService, 
                         TheaterEventService theaterEventService,
                         ExhibitionEventService exhibitionEventService,
                         ArtistService artistService,
                         VenueService venueService,
                         BiletixScraperConfig config,
                         BiletixEventExtractor eventExtractor) {
        LOGGER.info("Initializing BiletixScraper");
        this.concertEventService = concertEventService;
        this.theaterEventService = theaterEventService;
        this.exhibitionEventService = exhibitionEventService;
        this.artistService = artistService;
        this.venueService = venueService;
        this.config = config;
        this.eventExtractor = eventExtractor;
        LOGGER.info("BiletixScraper initialized successfully");
    }

    private void initializeBrowser() {
        LOGGER.info("Initializing browser");
        this.driver = WebScraperUtils.initializeBrowser(config.getChromePath(), config.getWaitTimeoutSeconds());
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(config.getWaitTimeoutSeconds()));
        LOGGER.info("Browser initialized successfully");
    }

    private void closeBrowser() {
        WebScraperUtils.closeBrowser(driver);
    }


    @Override
    public List<Event> fetchEvents() {
        LOGGER.info("Starting to fetch events from Biletix");
        List<Event> allEvents = new ArrayList<>();

        try {
            LOGGER.info("Processing MUSIC category");
            List<Event> musicEvents = processCategory(BiletixCategory.MUSIC);
            allEvents.addAll(musicEvents);

            LOGGER.info("Processing ART category");
            List<Event> artEvents = processCategory(BiletixCategory.ART);
            allEvents.addAll(artEvents);

            LOGGER.info("Successfully fetched and saved " + allEvents.size() + " events from Biletix");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching events from Biletix", e);
        }

        return allEvents;
    }

    private List<Event> processCategory(BiletixCategory category) {
        List<Event> events = new ArrayList<>();
        try {
            initializeBrowser();

            LOGGER.info("Fetching events for category: " + category.name());
            List<Event> categoryEvents = fetchEventsByCategory(category);

            saveEventsWithDuplicateChecking(categoryEvents);
            events.addAll(categoryEvents);

            LOGGER.info("Processed " + events.size() + " events for category: " + category.name());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing category: " + category.name(), e);
        } finally {
            closeBrowser();
        }
        return events;
    }

    private void saveEventsWithDuplicateChecking(List<Event> events) {
        for (Event event : events) {
            try {
                if (event instanceof ConcertEvent) {
                    saveConcertEventWithDuplicateChecking((ConcertEvent) event);
                } else if (event instanceof PerformingArt) {
                    savePerformingArtWithDuplicateChecking((PerformingArt) event);
                } else if (event instanceof ExhibitionEvent) {
                    saveExhibitionEventWithDuplicateChecking((ExhibitionEvent) event);
                } else {
                    LOGGER.warning("Unknown event type: " + event.getClass().getName());
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error saving event: " + event.getTitle(), e);
            }
        }
    }


    private void saveConcertEventWithDuplicateChecking(ConcertEvent event) {
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

    private void savePerformingArtWithDuplicateChecking(PerformingArt event) {
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


    private void saveExhibitionEventWithDuplicateChecking(ExhibitionEvent event) {
        LOGGER.info("Saving exhibition event: " + event.getTitle());
        exhibitionEventService.save(event);
    }


    private List<Event> fetchEventsByCategory(BiletixCategory category) {
        LOGGER.info("Fetching events for category: " + category.name());
        List<Event> events = new ArrayList<>();

        try {
            String searchUrl = buildSearchUrl(category);
            navigateToUrl(searchUrl);

            EventCollection eventCollection = collectEventCardsAndUrls();

            events = extractBasicInfoFromCards(eventCollection.getEventCards(), category);

            LOGGER.info("Successfully fetched " + events.size() + " events for category: " + category.name());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching events for category: " + category.name(), e);
        }

        return events;
    }

    private String buildSearchUrl(BiletixCategory category) {
        return String.format(
                "%s/search/TURKIYE/tr?category_sb=%s&date_sb=%s&city_sb=%s#!category_sb:%s,city_sb:%s,date_sb:%s",
                BASE_URL,
                category.getValue(),
                DEFAULT_DATE.getValue(),
                DEFAULT_CITY.getValue(),
                category.getValue(),
                DEFAULT_CITY.getValue(),
                DEFAULT_DATE.getValue()
        );
    }

    private void navigateToUrl(String url) {
        LOGGER.fine("Navigating to: " + url);
        driver.get(url);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
    }


    private EventCollection collectEventCardsAndUrls() {
        LOGGER.fine("Collecting event cards and URLs");
        clickLoadMoreButton();

        List<WebElement> eventDivs = driver.findElements(By.cssSelector(EVENT_CARD_SELECTOR));
        List<String> eventUrls = new ArrayList<>();
        List<WebElement> eventCards = new ArrayList<>();

        for (WebElement card : eventDivs) {
            try {
                String eventUrl = findEventUrl(card);

                if (eventUrl != null && !eventUrl.isEmpty()) {
                    eventUrls.add(eventUrl);
                    eventCards.add(card);
                } else {
                    LOGGER.warning("Could not find event URL in card");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error processing event card", e);
            }
        }

        LOGGER.info("Collected " + eventCards.size() + " event cards");
        return new EventCollection(eventCards, eventUrls);
    }


    private void clickLoadMoreButton() {
        LOGGER.info("Starting to click 'Load More' button to load all events");
        int attempts = 0;
        int totalEventsLoaded = 0;
        int previousEventCount = 0;

        // First, wait for the initial page to load completely
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(EVENT_CARD_SELECTOR)));
            previousEventCount = driver.findElements(By.cssSelector(EVENT_CARD_SELECTOR)).size();
            LOGGER.info("Initial page loaded with " + previousEventCount + " events");

            // Try to handle cookie consent banner and overlays proactively
            handleCookieConsent();
            handleOverlays();

            // Wait a moment for the DOM to settle after handling overlays
            Thread.sleep(1000);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error waiting for initial page to load", e);
            return;
        }

        while (attempts < config.getMaxLoadMoreAttempts()) {
            try {
                List<WebElement> loadMoreButtons = driver.findElements(By.cssSelector(LOAD_MORE_BUTTON_SELECTOR));

                if (loadMoreButtons.isEmpty()) {
                    LOGGER.info("No more 'Load More' buttons found. All events loaded.");
                    break;
                }

                WebElement loadMoreButton = loadMoreButtons.get(0);
                LOGGER.fine("Found 'Load More' button with text: " + loadMoreButton.getText());

                boolean clickSuccess = false;

                try {
                    scrollToElement(loadMoreButton);
                    loadMoreButton.click();
                    clickSuccess = true;
                    LOGGER.fine("Clicked 'Load More' button after scrolling, attempt " + (attempts + 1));
                } catch (Exception e) {
                    LOGGER.fine("Direct click after scroll failed, trying JavaScript click");
                }

                if (!clickSuccess) {
                    try {
                        clickElementWithJavaScript(loadMoreButton);
                        clickSuccess = true;
                        LOGGER.fine("Clicked 'Load More' button with JavaScript, attempt " + (attempts + 1));
                    } catch (Exception e) {
                        LOGGER.fine("JavaScript click failed, trying to handle overlays");
                    }
                }

                if (!clickSuccess) {
                    handleOverlays();
                    try {
                        loadMoreButton = driver.findElement(By.cssSelector(LOAD_MORE_BUTTON_SELECTOR));
                        clickElementWithJavaScript(loadMoreButton);
                        clickSuccess = true;
                        LOGGER.fine("Clicked 'Load More' button after handling overlays, attempt " + (attempts + 1));
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "All click strategies failed on attempt " + attempts, e);
                    }
                }

                if (!clickSuccess) {
                    LOGGER.warning("Failed to click 'Load More' button on attempt " + attempts + " after trying all strategies");
                    attempts++;
                    continue;
                }

                Thread.sleep(config.getLoadMoreWaitMs());

                int currentEventCount = driver.findElements(By.cssSelector(EVENT_CARD_SELECTOR)).size();
                int newEvents = currentEventCount - previousEventCount;

                LOGGER.info("After clicking 'Load More', now have " + currentEventCount + " events (+" + newEvents + ")");

                if (newEvents <= 0 && attempts > 0) {
                    LOGGER.info("No new events loaded after clicking 'Load More'. Might have reached the end.");
                    if (attempts > 1) {
                        break;
                    }
                }

                previousEventCount = currentEventCount;
                attempts++;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in 'Load More' button click process on attempt " + attempts, e);

                try {
                    handleOverlays();
                    Thread.sleep(1000);
                } catch (Exception ex) {
                }

                attempts++;
            }
        }

        if (attempts >= config.getMaxLoadMoreAttempts()) {
            LOGGER.warning("Reached maximum number of 'Load More' attempts (" + config.getMaxLoadMoreAttempts() + ")");
        }

        LOGGER.info("Finished loading additional events. Total events loaded: " + 
                    driver.findElements(By.cssSelector(EVENT_CARD_SELECTOR)).size());
    }

    /**
     * Scrolls to make an element visible in the viewport
     */
    private void scrollToElement(WebElement element) {
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
            // Add a small delay to allow the page to settle after scrolling
            Thread.sleep(500);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error scrolling to element", e);
        }
    }

    /**
     * Clicks an element using JavaScript
     */
    private void clickElementWithJavaScript(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    /**
     * Attempts to handle cookie consent banners and other overlays
     */
    private void handleCookieConsent() {
        try {
            String[] consentSelectors = {
                "#onetrust-accept-btn-handler",  // OneTrust
                ".ot-sdk-container button.accept-cookies",
                ".ot-sdk-container .ot-sdk-button",
                ".cookie-consent-button",
                ".accept-cookies",
                ".cookie-accept",
                "button[aria-label='Accept cookies']"
            };

            String[] xpathSelectors = {
                "//button[contains(text(), 'Accept')]",
                "//button[contains(text(), 'Kabul')]",  // Turkish for "Accept"
                "//a[contains(text(), 'Accept')]",
                "//a[contains(text(), 'Kabul')]"
            };

            for (String selector : consentSelectors) {
                try {
                    List<WebElement> consentButtons = driver.findElements(By.cssSelector(selector));
                    if (!consentButtons.isEmpty()) {
                        WebElement consentButton = consentButtons.get(0);
                        clickElementWithJavaScript(consentButton);
                        LOGGER.info("Clicked cookie consent button with CSS selector: " + selector);
                        Thread.sleep(1000); // Wait for banner to disappear
                        return;
                    }
                } catch (Exception e) {
                }
            }

            for (String xpath : xpathSelectors) {
                try {
                    List<WebElement> consentButtons = driver.findElements(By.xpath(xpath));
                    if (!consentButtons.isEmpty()) {
                        WebElement consentButton = consentButtons.get(0);
                        clickElementWithJavaScript(consentButton);
                        LOGGER.info("Clicked cookie consent button with XPath: " + xpath);
                        Thread.sleep(1000); // Wait for banner to disappear
                        return;
                    }
                } catch (Exception e) {
                }
            }

            List<WebElement> otSdkRows = driver.findElements(By.cssSelector("div.ot-sdk-row"));
            if (!otSdkRows.isEmpty()) {
                try {
                    WebElement otSdkRow = otSdkRows.get(0);
                    List<WebElement> buttons = otSdkRow.findElements(By.tagName("button"));
                    if (!buttons.isEmpty()) {
                        clickElementWithJavaScript(buttons.get(0));
                        LOGGER.info("Clicked button in ot-sdk-row");
                        Thread.sleep(1000);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.FINE, "Failed to interact with ot-sdk-row", e);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error handling cookie consent", e);
        }
    }

    private void handleOverlays() {
        try {
            String otSdkRowScript =
                "var otRows = document.querySelectorAll('.ot-sdk-row');" +
                "for(var i=0; i<otRows.length; i++) {" +
                "  otRows[i].style.display = 'none';" +
                "  otRows[i].style.zIndex = '-1000';" +
                "  otRows[i].style.pointerEvents = 'none';" +  // This prevents the element from receiving clicks
                "}";
            ((JavascriptExecutor) driver).executeScript(otSdkRowScript);

            String oneTrustScript =
                "var otContainers = document.querySelectorAll('#onetrust-banner-sdk, #onetrust-consent-sdk, .onetrust-pc-dark-filter');" +
                "for(var i=0; i<otContainers.length; i++) {" +
                "  otContainers[i].style.display = 'none';" +
                "  otContainers[i].style.zIndex = '-1000';" +
                "  otContainers[i].style.pointerEvents = 'none';" +
                "}";
            ((JavascriptExecutor) driver).executeScript(oneTrustScript);

            String removeOverlaysScript =
                "var overlays = document.querySelectorAll('.cookie-banner, .modal, .popup, .overlay, [class*=\"cookie\"], [id*=\"cookie\"], [class*=\"consent\"], [id*=\"consent\"]);" +
                "for(var i=0; i<overlays.length; i++) {" +
                "  overlays[i].style.display = 'none';" +
                "  overlays[i].style.zIndex = '-1000';" +
                "  overlays[i].style.pointerEvents = 'none';" +
                "}";
            ((JavascriptExecutor) driver).executeScript(removeOverlaysScript);

            String makeButtonClickableScript =
                "var loadMoreButtons = document.querySelectorAll('.search_load_more');" +
                "for(var i=0; i<loadMoreButtons.length; i++) {" +
                "  loadMoreButtons[i].style.position = 'relative';" +
                "  loadMoreButtons[i].style.zIndex = '10000';" +
                "}";
            ((JavascriptExecutor) driver).executeScript(makeButtonClickableScript);

            Thread.sleep(500);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error handling overlays", e);
        }
    }


    private String findEventUrl(WebElement card) {
        String eventUrl = null;

        try {
            WebElement nameElement = card.findElement(By.cssSelector(EVENT_TITLE_SELECTOR));
            eventUrl = nameElement.getAttribute("href");
        } catch (Exception ex) {
            LOGGER.finest("Could not find URL in title element");
        }

        if (eventUrl == null || eventUrl.isEmpty()) {
            try {
                WebElement linkElement = card.findElement(By.cssSelector(EVENT_LINK_SELECTOR));
                eventUrl = linkElement.getAttribute("href");
            } catch (Exception ex) {
                LOGGER.finest("Could not find URL in link element");
            }
        }

        return eventUrl;
    }


    private List<Event> extractBasicInfoFromCards(List<WebElement> eventCards, BiletixCategory category) {
        LOGGER.fine("Extracting basic info from event cards for category: " + category.name());
        List<Event> events = new ArrayList<>();

        for (WebElement card : eventCards) {
            Event event = extractBasicEventInfo(card, category);
            if (event != null) {
                events.add(event);
            }
        }

        LOGGER.info("Extracted basic info for " + events.size() + " events for category: " + category.name());
        return events;
    }

    private Event extractBasicEventInfo(WebElement card, BiletixCategory category) {
        try {
            EventInfo info = extractCardInfo(card);
            if (info == null) {
                LOGGER.warning("Failed to extract event info from card");
                return null;
            }

            if (info.getTitle().isEmpty()) {
                LOGGER.warning("Cannot create event without a title");
                return null;
            }

            EventType eventType = determineEventType(info.getTitle(), "", category);
            return createEventFromInfo(eventType, info);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error extracting basic event info", e);
            return null;
        }
    }


    private EventInfo extractCardInfo(WebElement card) {
        String title = extractTitle(card);
        String dateStr = extractDate(card);
        String venueName = extractVenueName(card);
        String location = extractLocation(card);
        String ticketUrl = findEventUrl(card);
        if (Objects.equals(dateStr, "")) {
            return null;
        }
        return new EventInfo(title, dateStr, venueName, location, ticketUrl);
    }


    private String extractTitle(WebElement card) {
        try {
            WebElement titleElement = card.findElement(By.cssSelector(EVENT_TITLE_SELECTOR));
            return titleElement.getText();
        } catch (Exception e) {
            try {
                return card.findElement(By.cssSelector(EVENT_TITLE_MOBILE_SELECTOR)).getText();
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Error extracting event title", ex);
                return "";
            }
        }
    }


    private String extractDate(WebElement card) {
        try {
            String secondDate = card.findElement(By.cssSelector(EVENT_SECONDARY_DATE_SELECTOR)).getText();
            String firstDate = card.findElement(By.cssSelector(EVENT_DATE_SELECTOR)).getText();
            return Objects.equals(secondDate, "") ? firstDate : "";
        } catch (Exception e) {
            try {
                String day = card.findElement(By.cssSelector(".searchMobileDateDayNumber")).getText();
                String month = card.findElement(By.cssSelector(".searchMobileDateMonth")).getText();
                String year = card.findElement(By.cssSelector(".searchMobileDateYear")).getText();


                return  day + "/" + getMonthNumber(month) + "/" + year;
            } catch (Exception ex) {
                LOGGER.log(Level.FINE, "Error extracting event date", ex);
                return "";
            }
        }
    }

    private String extractVenueName(WebElement card) {
        try {
            return card.findElement(By.cssSelector(EVENT_VENUE_SELECTOR)).getText();
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error extracting venue name", e);
            return "";
        }
    }


    private String extractLocation(WebElement card) {
        try {
            return card.findElement(By.cssSelector(EVENT_LOCATION_SELECTOR)).getText();
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error extracting location", e);
            return "";
        }
    }


    private Event createEventFromInfo(EventType eventType, EventInfo info) {
        switch (eventType) {
            case CONCERT:
                return createConcertEvent(info);
            case THEATER:
                return createTheaterEvent(info, eventType);
            case STANDUP:
                return createTheaterEvent(info, eventType);
            case EXHIBITION:
                return createExhibitionEvent(info);
            default:
                LOGGER.warning("Unknown event type: " + eventType);
                return null;
        }
    }


    private <T extends Event> void setCommonEventProperties(T event, String title, String description, String dateStr, String ticketUrl) {
        event.setTitle(title);
        event.setDescription(description);

        if (dateStr != null && !dateStr.isEmpty()) {
            event.setStartDate(parseDate(dateStr));
        }

        event.setSource(EventSourceType.BILETIX);

        if (ticketUrl != null && !ticketUrl.isEmpty()) {
            event.setTicketUrl(ticketUrl);
        }
    }

    private void setVenueProperties(ConcertEvent event, String venueName, String location) {
        if ((venueName != null && !venueName.isEmpty()) || (location != null && !location.isEmpty())) {
            Venue venue = new Venue();
            venue.setName(venueName);
            venue.setLocation(location);
            event.setVenue(venue);
        }
    }

    private ConcertEvent createConcertEvent(EventInfo info) {
        ConcertEvent concert = new ConcertEvent();
        setCommonEventProperties(concert, info.getTitle(), "", info.getDateStr(), info.getTicketUrl());

        Artist artist = new Artist();
        artist.setName(extractArtistName(info.getTitle(), ""));
        concert.setArtist(artist);

        setVenueProperties(concert, info.getVenueName(), info.getLocation());

        return concert;
    }

    private PerformingArt createTheaterEvent(EventInfo info, EventType eventType) {
        PerformingArt theater = new PerformingArt();
        setCommonEventProperties(theater, info.getTitle(), "", info.getDateStr(), info.getTicketUrl());

        if (eventType == EventType.STANDUP) {
            theater.setPerformanceType(PerformanceType.STANDUP);
        } else {
            theater.setPerformanceType(PerformanceType.THEATER);
        }

        return theater;
    }

    private ExhibitionEvent createExhibitionEvent(EventInfo info) {
        ExhibitionEvent exhibition = new ExhibitionEvent();
        setCommonEventProperties(exhibition, info.getTitle(), "", info.getDateStr(), info.getTicketUrl());

        return exhibition;
    }


    private String extractTextWithMultipleSelectors(String elementType, String... selectors) {
        LOGGER.fine("Extracting " + elementType);
        try {
            for (String selector : selectors) {
                try {
                    return driver.findElement(By.cssSelector(selector)).getText();
                } catch (Exception e) {
                }
            }

            if (elementType.equals("title")) {
                return driver.getTitle().replace(" - Biletix", "").trim();
            }

            return "";
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error extracting " + elementType, e);
            return "";
        }
    }

    private String extractDetailedTitle() {
        return extractTextWithMultipleSelectors("title", 
            "h1.eventTitle", 
            ".eventTitle", 
            "h1"
        );
    }

    private String extractDetailedDescription() {
        return extractTextWithMultipleSelectors("description", 
            ".eventDescription", 
            ".description", 
            "div[class*='description']"
        );
    }

    private String extractDetailedDate() {
        return extractTextWithMultipleSelectors("date", 
            ".eventDate", 
            ".date", 
            "div[class*='date']"
        );
    }

    private String extractDetailedVenueName() {
        return extractTextWithMultipleSelectors("venue name", 
            ".eventVenue", 
            ".venue", 
            "div[class*='venue']"
        );
    }

    private String extractDetailedLocation() {
        return extractTextWithMultipleSelectors("location", 
            ".eventLocation", 
            ".location", 
            "div[class*='location']"
        );
    }


    private Event createDetailedEventFromInfo(EventType eventType, DetailedEventInfo info) {
        switch (eventType) {
            case CONCERT:
                return createDetailedConcertEvent(info);
            case THEATER:
                return createDetailedTheaterEvent(info, eventType);
            case STANDUP:
                return createDetailedTheaterEvent(info, eventType);
            case EXHIBITION:
                return createDetailedExhibitionEvent(info);
            default:
                LOGGER.warning("Unknown event type: " + eventType);
                return null;
        }
    }

    private ConcertEvent createDetailedConcertEvent(DetailedEventInfo info) {
        ConcertEvent concert = new ConcertEvent();
        setCommonEventProperties(concert, info.getTitle(), info.getDescription(), info.getDateStr(), info.getTicketUrl());

        Artist artist = new Artist();
        artist.setName(extractArtistName(info.getTitle(), info.getDescription()));
        concert.setArtist(artist);

        setVenueProperties(concert, info.getVenueName(), info.getLocation());

        return concert;
    }

    private PerformingArt createDetailedTheaterEvent(DetailedEventInfo info, EventType eventType) {
        PerformingArt theater = new PerformingArt();
        setCommonEventProperties(theater, info.getTitle(), info.getDescription(), info.getDateStr(), info.getTicketUrl());

        if (eventType == EventType.STANDUP) {
            theater.setPerformanceType(PerformanceType.STANDUP);
        } else {
            theater.setPerformanceType(PerformanceType.THEATER);
        }

        return theater;
    }

    private ExhibitionEvent createDetailedExhibitionEvent(DetailedEventInfo info) {
        ExhibitionEvent exhibition = new ExhibitionEvent();
        setCommonEventProperties(exhibition, info.getTitle(), info.getDescription(), info.getDateStr(), info.getTicketUrl());

        return exhibition;
    }

    private EventType determineEventType(String title, String description, BiletixCategory category) {
        if (category == BiletixCategory.ART) {
            String lowerTitle = title.toLowerCase();
            String lowerDesc = description.toLowerCase();

            if (lowerTitle.contains("stand") || lowerDesc.contains("stand up") || lowerTitle.contains("stand-up") || lowerDesc.contains("stand-up")) {
                return EventType.STANDUP;
            } else {
                return EventType.THEATER;
            }
        } else if (category == BiletixCategory.MUSIC) {
            return EventType.CONCERT;
        } else {
            String lowerTitle = title.toLowerCase();
            String lowerDesc = description.toLowerCase();

            if (lowerTitle.contains("konser") || lowerDesc.contains("konser")) {
                return EventType.CONCERT;
            } else if (lowerTitle.contains("tiyatro") || lowerDesc.contains("tiyatro")) {
                return EventType.THEATER;
            } else if (lowerTitle.contains("sergi") || lowerDesc.contains("sergi")) {
                return EventType.EXHIBITION;
            }

            LOGGER.fine("Could not determine event type from title/description, defaulting to concert");
            return EventType.CONCERT;
        }
    }


    private EventType determineEventType(String title, String description) {
        return determineEventType(title, description, BiletixCategory.MUSIC);
    }


    private String extractArtistName(String title, String description) {
        if (title.contains("-")) {
            return title.split("-")[0].trim();
        }

        return title.trim();
    }

    private LocalDate parseDate(String dateStr) {
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
                }
                else if (dateStr.contains("/")) {
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

    private String getMonthNumber(String monthName) {
        if (monthName == null || monthName.isEmpty()) {
            LOGGER.warning("Empty month name provided");
            return "01"; // Default to January
        }

        String lowerMonth = monthName.toLowerCase();

        switch (lowerMonth) {
            case "oca":
                return "01"; // Ocak (January)
            case "şub":
                return "02"; // Şubat (February)
            case "mar":
                return "03"; // Mart (March)
            case "nis":
                return "04"; // Nisan (April)
            case "may":
                return "05"; // Mayıs (May)
            case "haz":
                return "06"; // Haziran (June)
            case "tem":
                return "07"; // Temmuz (July)
            case "ağu":
                return "08"; // Ağustos (August)
            case "eyl":
                return "09"; // Eylül (September)
            case "eki":
                return "10"; // Ekim (October)
            case "kas":
                return "11"; // Kasım (November)
            case "ara":
                return "12"; // Aralık (December)
            default:
                LOGGER.warning("Unknown month abbreviation: " + monthName);
                return "01"; // Default to January if unknown
        }
    }


    @Override
    public String getSourceName() {
        return "Biletix";
    }

    @Override
    public boolean isAvailable() {
        try {
            LOGGER.fine("Checking if Biletix is available");
            driver.get(BASE_URL);
            boolean available = driver.getTitle().contains("Biletix");
            LOGGER.info("Biletix availability check: " + available);
            return available;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error checking Biletix availability", e);
            return false;
        }
    }

    @Override
    public String getSourceUrl() {
        return BASE_URL;
    }
}
