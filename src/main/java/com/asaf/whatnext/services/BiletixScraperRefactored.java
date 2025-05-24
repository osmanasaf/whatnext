package com.asaf.whatnext.services;

import com.asaf.whatnext.config.BiletixScraperConfig;
import com.asaf.whatnext.enums.Biletix.BiletixCategory;
import com.asaf.whatnext.models.ConcertEvent;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.models.ExhibitionEvent;
import com.asaf.whatnext.models.PerformingArt;
import com.asaf.whatnext.service.ArtistService;
import com.asaf.whatnext.service.ConcertEventService;
import com.asaf.whatnext.service.ExhibitionEventService;
import com.asaf.whatnext.service.TheaterEventService;
import com.asaf.whatnext.service.VenueService;
import com.asaf.whatnext.utils.EventUtils;
import com.asaf.whatnext.utils.WebScraperUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class BiletixScraperRefactored implements EventSource {
    private static final Logger LOGGER = Logger.getLogger(BiletixScraperRefactored.class.getName());

    private final ConcertEventService concertEventService;
    private final TheaterEventService theaterEventService;
    private final ExhibitionEventService exhibitionEventService;
    private final ArtistService artistService;
    private final VenueService venueService;
    private final BiletixScraperConfig config;
    private final BiletixEventExtractor eventExtractor;

    private WebDriver driver;
    private WebDriverWait wait;

    @Autowired
    public BiletixScraperRefactored(ConcertEventService concertEventService,
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
            // Process MUSIC category
            LOGGER.info("Processing MUSIC category");
            List<Event> musicEvents = processCategory(BiletixCategory.MUSIC);
            allEvents.addAll(musicEvents);

            // Process ART category
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
            // Initialize browser for this category
            initializeBrowser();

            // Fetch events for the category
            LOGGER.info("Fetching events for category: " + category.name());
            List<Event> categoryEvents = fetchEventsByCategory(category);

            // Save events with duplicate checking
            saveEventsWithDuplicateChecking(categoryEvents);
            events.addAll(categoryEvents);

            LOGGER.info("Processed " + events.size() + " events for category: " + category.name());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing category: " + category.name(), e);
        } finally {
            // Close browser after processing this category
            closeBrowser();
        }
        return events;
    }

    private void saveEventsWithDuplicateChecking(List<Event> events) {
        for (Event event : events) {
            try {
                if (event instanceof ConcertEvent) {
                    EventUtils.saveConcertEventWithDuplicateChecking((ConcertEvent) event, 
                                                                   concertEventService, 
                                                                   artistService, 
                                                                   venueService);
                } else if (event instanceof PerformingArt) {
                    EventUtils.savePerformingArtWithDuplicateChecking((PerformingArt) event, theaterEventService);
                } else if (event instanceof ExhibitionEvent) {
                    EventUtils.saveExhibitionEventWithDuplicateChecking((ExhibitionEvent) event, exhibitionEventService);
                } else {
                    LOGGER.warning("Unknown event type: " + event.getClass().getName());
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error saving event: " + event.getTitle(), e);
            }
        }
    }

    private List<Event> fetchEventsByCategory(BiletixCategory category) {
        LOGGER.info("Fetching events for category: " + category.name());
        List<Event> events = new ArrayList<>();

        try {
            String searchUrl = config.buildSearchUrl(category);
            WebScraperUtils.navigateToUrl(driver, wait, searchUrl);

            EventCollection eventCollection = collectEventCardsAndUrls();
            events = eventExtractor.extractBasicInfoFromCards(eventCollection.getEventCards(), category);

            LOGGER.info("Successfully fetched " + events.size() + " events for category: " + category.name());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching events for category: " + category.name(), e);
        }

        return events;
    }

    private EventCollection collectEventCardsAndUrls() {
        LOGGER.fine("Collecting event cards and URLs");

        // Click "Load More" button until all events are loaded or max attempts reached
        clickLoadMoreButton();

        // Now collect all event cards
        List<WebElement> eventDivs = driver.findElements(By.cssSelector(BiletixScraperConfig.EVENT_CARD_SELECTOR));
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
        int previousEventCount = 0;

        // First, wait for the initial page to load completely
        try {
            wait.until(driver -> !driver.findElements(By.cssSelector(BiletixScraperConfig.EVENT_CARD_SELECTOR)).isEmpty());
            previousEventCount = driver.findElements(By.cssSelector(BiletixScraperConfig.EVENT_CARD_SELECTOR)).size();
            LOGGER.info("Initial page loaded with " + previousEventCount + " events");

            // Try to handle cookie consent banner and overlays proactively
            WebScraperUtils.handleCookieConsent(driver);
            WebScraperUtils.handleOverlays(driver);

            // Wait a moment for the DOM to settle after handling overlays
            Thread.sleep(1000);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error waiting for initial page to load", e);
            return;
        }

        while (attempts < config.getMaxLoadMoreAttempts()) {
            try {
                // Check if the "Load More" button exists
                List<WebElement> loadMoreButtons = driver.findElements(By.cssSelector(BiletixScraperConfig.LOAD_MORE_BUTTON_SELECTOR));

                if (loadMoreButtons.isEmpty()) {
                    LOGGER.info("No more 'Load More' buttons found. All events loaded.");
                    break;
                }

                // Get the button
                WebElement loadMoreButton = loadMoreButtons.get(0);
                LOGGER.fine("Found 'Load More' button with text: " + loadMoreButton.getText());

                // Try multiple click strategies
                boolean clickSuccess = false;

                // Strategy 1: Try scrolling to the button first
                try {
                    WebScraperUtils.scrollToElement(driver, loadMoreButton);
                    loadMoreButton.click();
                    clickSuccess = true;
                    LOGGER.fine("Clicked 'Load More' button after scrolling, attempt " + (attempts + 1));
                } catch (Exception e) {
                    LOGGER.fine("Direct click after scroll failed, trying JavaScript click");
                }

                // Strategy 2: Try JavaScript click if direct click failed
                if (!clickSuccess) {
                    try {
                        WebScraperUtils.clickElementWithJavaScript(driver, loadMoreButton);
                        clickSuccess = true;
                        LOGGER.fine("Clicked 'Load More' button with JavaScript, attempt " + (attempts + 1));
                    } catch (Exception e) {
                        LOGGER.fine("JavaScript click failed, trying to handle overlays");
                    }
                }

                // Strategy 3: Try to handle any overlays and retry
                if (!clickSuccess) {
                    WebScraperUtils.handleOverlays(driver);
                    try {
                        loadMoreButton = driver.findElement(By.cssSelector(BiletixScraperConfig.LOAD_MORE_BUTTON_SELECTOR));
                        WebScraperUtils.clickElementWithJavaScript(driver, loadMoreButton);
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

                // Wait for new content to load
                Thread.sleep(config.getLoadMoreWaitMs());

                // Count current number of events for logging
                int currentEventCount = driver.findElements(By.cssSelector(BiletixScraperConfig.EVENT_CARD_SELECTOR)).size();
                int newEvents = currentEventCount - previousEventCount;

                LOGGER.info("After clicking 'Load More', now have " + currentEventCount + " events (+" + newEvents + ")");

                // If no new events were loaded, we might have reached the end
                if (newEvents <= 0 && attempts > 0) {
                    LOGGER.info("No new events loaded after clicking 'Load More'. Might have reached the end.");
                    // Try one more time to be sure
                    if (attempts > 1) {
                        break;
                    }
                }

                previousEventCount = currentEventCount;
                attempts++;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in 'Load More' button click process on attempt " + attempts, e);

                // Try handling overlays again before the next attempt
                try {
                    WebScraperUtils.handleOverlays(driver);
                    Thread.sleep(1000);
                } catch (Exception ex) {
                    // Ignore any errors in overlay handling
                }

                attempts++;
            }
        }

        if (attempts >= config.getMaxLoadMoreAttempts()) {
            LOGGER.warning("Reached maximum number of 'Load More' attempts (" + config.getMaxLoadMoreAttempts() + ")");
        }

        LOGGER.info("Finished loading additional events. Total events loaded: " + 
                    driver.findElements(By.cssSelector(BiletixScraperConfig.EVENT_CARD_SELECTOR)).size());
    }

    private String findEventUrl(WebElement card) {
        String eventUrl = null;

        try {
            WebElement nameElement = card.findElement(By.cssSelector(BiletixScraperConfig.EVENT_TITLE_SELECTOR));
            eventUrl = nameElement.getAttribute("href");
        } catch (Exception ex) {
            LOGGER.finest("Could not find URL in title element");
        }

        if (eventUrl == null || eventUrl.isEmpty()) {
            try {
                WebElement linkElement = card.findElement(By.cssSelector(BiletixScraperConfig.EVENT_LINK_SELECTOR));
                eventUrl = linkElement.getAttribute("href");
            } catch (Exception ex) {
                LOGGER.finest("Could not find URL in link element");
            }
        }

        return eventUrl;
    }

    @Override
    public String getSourceName() {
        return "Biletix";
    }

    @Override
    public boolean isAvailable() {
        try {
            LOGGER.fine("Checking if Biletix is available");
            initializeBrowser();
            driver.get(BiletixScraperConfig.BASE_URL);
            boolean available = driver.getTitle().contains("Biletix");
            LOGGER.info("Biletix availability check: " + available);
            closeBrowser();
            return available;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error checking Biletix availability", e);
            closeBrowser();
            return false;
        }
    }

    @Override
    public String getSourceUrl() {
        return BiletixScraperConfig.BASE_URL;
    }
}