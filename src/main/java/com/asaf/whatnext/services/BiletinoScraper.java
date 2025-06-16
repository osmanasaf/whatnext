package com.asaf.whatnext.services;

import com.asaf.whatnext.config.BiletinoScraperConfig;
import com.asaf.whatnext.enums.Biletino.BiletinoCategory;
import com.asaf.whatnext.enums.Biletino.BiletinoCity;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.enums.PerformanceType;
import com.asaf.whatnext.models.*;
import com.asaf.whatnext.service.ArtistService;
import com.asaf.whatnext.service.ConcertEventService;
import com.asaf.whatnext.service.TheaterEventService;
import com.asaf.whatnext.service.VenueService;
import com.asaf.whatnext.utils.EventUtils;
import com.asaf.whatnext.utils.WebScraperUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.asaf.whatnext.config.BiletinoScraperConfig.*;
import static com.asaf.whatnext.utils.EventUtils.determineEventType;

@Service
public class BiletinoScraper implements EventSource {
    private static final Logger LOGGER = Logger.getLogger(BiletinoScraper.class.getName());
    private static final String IMAGE_SELECTOR = "img.swiper-lazy";
    
    private final ConcertEventService concertEventService;
    private final TheaterEventService theaterEventService;
    private final ArtistService artistService;
    private final VenueService venueService;
    private final BiletinoScraperConfig config;
    private final BiletinoCity city = BiletinoCity.ISTANBUL;
    private WebDriver driver;
    private WebDriverWait wait;

    @Autowired
    public BiletinoScraper(ConcertEventService concertEventService,
                           TheaterEventService theaterEventService,
                           ArtistService artistService,
                           VenueService venueService,
                           BiletinoScraperConfig config) {
        this.concertEventService = concertEventService;
        this.theaterEventService = theaterEventService;
        this.artistService = artistService;
        this.venueService = venueService;
        this.config = config;
    }

    private void initializeBrowser() {
        this.driver = WebScraperUtils.initializeBrowser(config.getChromePath(), config.getWaitTimeoutSeconds());
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(config.getWaitTimeoutSeconds()));
    }

    private void closeBrowser() {
        WebScraperUtils.closeBrowser(driver);
    }

    @Override
    public List<Event> fetchEvents() {
        return fetchEvents(BiletinoCity.ISTANBUL.getValue());
    }

    @Override
    public List<Event> fetchEvents(String city) {
        List<Event> allEvents = new ArrayList<>();
        try {
            BiletinoCity selectedCity = Arrays.stream(BiletinoCity.values())
                    .filter(c -> c.getValue().equalsIgnoreCase(city))
                    .findFirst()
                    .orElse(BiletinoCity.ISTANBUL);

            allEvents.addAll(processCategory(BiletinoCategory.MUSIC, selectedCity));
            allEvents.addAll(processCategory(BiletinoCategory.THEATRE, selectedCity));
            allEvents.addAll(processCategory(BiletinoCategory.COMEDY, selectedCity));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching events from Biletino", e);
        }
        return allEvents;
    }

    public List<Event> processCategory(BiletinoCategory category) {
        return processCategory(category, BiletinoCity.ISTANBUL);
    }

    public List<Event> processCategory(BiletinoCategory category, BiletinoCity city) {
        List<Event> events = new ArrayList<>();
        try {
            initializeBrowser();
            List<Event> categoryEvents = fetchEventsByCategory(category, city);
            saveEventsWithDuplicateChecking(categoryEvents);
            events.addAll(categoryEvents);
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
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error saving event: " + event.getTitle(), e);
            }
        }
    }

    private void saveConcertEventWithDuplicateChecking(ConcertEvent event) {
        if (event.getArtist() == null || event.getArtist().getName() == null || event.getStartDate() == null) return;
        String artistName = event.getArtist().getName();
        LocalDate startDate = event.getStartDate();
        if (!concertEventService.existsByArtistNameAndDate(artistName, startDate)) {
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
        }
    }

    private void savePerformingArtWithDuplicateChecking(PerformingArt event) {
        if (event.getTitle() == null || event.getStartDate() == null || event.getPerformanceType() == null) return;
        String title = event.getTitle();
        LocalDate startDate = event.getStartDate();
        PerformanceType performanceType = event.getPerformanceType();
        if (!theaterEventService.existsByTitleDateAndType(title, startDate, performanceType)) {
            theaterEventService.save(event);
        }
    }

    private List<Event> fetchEventsByCategory(BiletinoCategory category) {
        return fetchEventsByCategory(category, BiletinoCity.ISTANBUL);
    }

    private List<Event> fetchEventsByCategory(BiletinoCategory category, BiletinoCity city) {
        List<Event> events = new ArrayList<>();
        try {
            String searchUrl = config.buildSearchUrl(category, city);
            navigateToUrl(searchUrl);
            handleCookieConsent();
            handleOverlays();
            scrollToLoadAllEvents();
            List<WebElement> eventCards = driver.findElements(By.cssSelector(EVENT_CARD_SELECTOR));
            List<String> eventCardsHtml = new ArrayList<>();
            for (WebElement card : eventCards) {
                eventCardsHtml.add(card.getAttribute("outerHTML"));
            }
            events = extractBasicInfoFromCards(eventCardsHtml, category);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching events for category: " + category.name(), e);
        }
        return events;
    }

    private void navigateToUrl(String url) {
        driver.get(url);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
    }

    private void scrollToLoadAllEvents() {
        int attempts = 0;
        int previousEventCount = 0;
        boolean endOfResultsFound = false;
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(EVENT_CARD_SELECTOR)));
            previousEventCount = driver.findElements(By.cssSelector(EVENT_CARD_SELECTOR)).size();
        } catch (Exception e) {
            return;
        }
        while (attempts < config.getMaxScrollAttempts() && !endOfResultsFound) {
            try {
                ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");
                Thread.sleep(config.getScrollWaitMs());
                List<WebElement> endOfResultsElements = driver.findElements(By.cssSelector(SEARCH_LOAD_END_SELECTOR));
                if (!endOfResultsElements.isEmpty() && endOfResultsElements.get(0).isDisplayed()) {
                    endOfResultsFound = true;
                    break;
                }
                int currentEventCount = driver.findElements(By.cssSelector(EVENT_CARD_SELECTOR)).size();
                int newEvents = currentEventCount - previousEventCount;
                if (newEvents <= 0 && attempts > 2) break;
                previousEventCount = currentEventCount;
                attempts++;
            } catch (Exception e) {
                attempts++;
            }
        }
    }

    private void handleCookieConsent() {
        String[] consentSelectors = {
                "#onetrust-accept-btn-handler",
                ".cookie-consent-button",
                ".accept-cookies",
                ".cookie-accept",
                "button[aria-label='Accept cookies']"
        };
        String[] xpathSelectors = {
                "//button[contains(text(), 'Accept')]",
                "//button[contains(text(), 'Kabul')]",
                "//a[contains(text(), 'Accept')]",
                "//a[contains(text(), 'Kabul')]"
        };
        for (String selector : consentSelectors) {
            try {
                List<WebElement> consentButtons = driver.findElements(By.cssSelector(selector));
                if (!consentButtons.isEmpty()) {
                    clickElementWithJavaScript(consentButtons.get(0));
                    Thread.sleep(1000);
                    return;
                }
            } catch (Exception ignored) {}
        }
        for (String xpath : xpathSelectors) {
            try {
                List<WebElement> consentButtons = driver.findElements(By.xpath(xpath));
                if (!consentButtons.isEmpty()) {
                    clickElementWithJavaScript(consentButtons.get(0));
                    Thread.sleep(1000);
                    return;
                }
            } catch (Exception ignored) {}
        }
    }

    private void handleOverlays() {
        try {
            String removeOverlaysScript =
                    "var overlays = document.querySelectorAll('.cookie-banner, .modal, .popup, .overlay, [class*=\"cookie\"], [id*=\"cookie\"], [class*=\"consent\"], [id*=\"consent\"]);" +
                            "for(var i=0; i<overlays.length; i++) {" +
                            "  overlays[i].style.display = 'none';" +
                            "  overlays[i].style.zIndex = '-1000';" +
                            "  overlays[i].style.pointerEvents = 'none';" +
                            "}";
            ((JavascriptExecutor) driver).executeScript(removeOverlaysScript);
            Thread.sleep(500);
        } catch (Exception ignored) {}
    }

    private void clickElementWithJavaScript(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    private List<Event> extractBasicInfoFromCards(List<String> eventCardsHtml, BiletinoCategory category) {
        List<Event> events = new ArrayList<>();
        for (String cardHtml : eventCardsHtml) {
            try {
                Document doc = org.jsoup.Jsoup.parse(cardHtml);
                EventInfo basicInfo = extractCardInfoFromHtml(doc);
                if (basicInfo == null || basicInfo.getTitle().isEmpty()) continue;
                String ticketUrl = basicInfo.getTicketUrl();
                if (ticketUrl == null || ticketUrl.isEmpty()) continue;
                DetailedEventInfo detailedInfo = fetchDetailedEventInfo(ticketUrl);
                EventType eventType = determineEventType(
                        basicInfo.getTitle(),
                        detailedInfo.getDescription(),
                        categoryToEventType(category)
                );

                Event event = createEventFromDetailedInfo(eventType, basicInfo, detailedInfo);
                if (event != null) {
                    // Extract and save image
                    try {
                        Element imgElement = doc.select(IMAGE_SELECTOR).first();
                        if (imgElement != null) {
                            String imageUrl = imgElement.attr("src");
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                String imageData = downloadImageAsBase64(imageUrl);
                                if (imageData != null) {
                                    event.setImage(imageData, "image/jpeg", imageUrl);
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error extracting image from card", e);
                    }
                    events.add(event);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error processing event card", e);
            }
        }
        return events;
    }

    private String downloadImageAsBase64(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            
            try (InputStream in = connection.getInputStream()) {
                byte[] imageBytes = in.readAllBytes();
                return Base64.getEncoder().encodeToString(imageBytes);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error downloading image: " + imageUrl, e);
            return null;
        }
    }

    public static EventType categoryToEventType(BiletinoCategory category) {
        switch (category) {
            case MUSIC:    return EventType.CONCERT;
            case THEATRE:  return EventType.THEATER;
            case COMEDY:   return EventType.STANDUP;
            default:       return EventType.CONCERT;
        }
    }


    private EventInfo extractCardInfoFromHtml(Document doc) {
        return EventInfo.builder()
                .title(doc.select(EVENT_TITLE_SELECTOR).text())
                .dateStr(doc.select(EVENT_DATE_SELECTOR).text())
                .location(doc.select(EVENT_LOCATION_SELECTOR).text())
                .ticketUrl(BASE_URL + doc.select(EVENT_LINK_SELECTOR).attr("href"))
                .build();
    }

    private DetailedEventInfo fetchDetailedEventInfo(String eventUrl) {
        try {
            driver.get(eventUrl);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            String title = "";
            try {
                WebElement titleElement = driver.findElement(By.cssSelector("h1.event-title"));
                title = titleElement.getText().trim();
            } catch (Exception ignored) {}
            String startDate = "";
            try {
                List<WebElement> startDateElements = driver.findElements(By.xpath("//label[contains(text(), 'Start Date')]/following-sibling::p"));
                if (!startDateElements.isEmpty()) {
                    startDate = startDateElements.get(0).getText().trim();
                } else {
                    startDateElements = driver.findElements(By.cssSelector("div.event-time-summary p.mb-1"));
                    if (!startDateElements.isEmpty()) {
                        startDate = startDateElements.get(0).getText().trim();
                    }
                }
            } catch (Exception ignored) {}
            String endDate = "";
            try {
                List<WebElement> endDateElements = driver.findElements(By.xpath("//label[contains(text(), 'End Date')]/following-sibling::p"));
                if (!endDateElements.isEmpty()) {
                    endDate = endDateElements.get(0).getText().trim();
                } else {
                    List<WebElement> dateElements = driver.findElements(By.cssSelector("div.event-time-summary p.mb-1"));
                    if (dateElements.size() > 1) {
                        endDate = dateElements.get(1).getText().trim();
                    }
                }
            } catch (Exception ignored) {}
            String location = "";
            String venueName = "";
            try {
                List<WebElement> locationElements = driver.findElements(By.xpath("//label[contains(text(), 'Location')]/following-sibling::p"));
                if (!locationElements.isEmpty()) {
                    String fullLocation = locationElements.get(0).getText().trim();
                    String[] result = processLocationString(fullLocation);
                    venueName = result[0];
                    location = result[1];
                } else {
                    locationElements = driver.findElements(By.cssSelector("div.event-time-summary p.mb-1"));
                    if (locationElements.size() > 2) {
                        String fullLocation = locationElements.get(2).getText().trim();
                        String[] result = processLocationString(fullLocation);
                        venueName = result[0];
                        location = result[1];
                    } else {
                        locationElements = driver.findElements(By.xpath("//label[contains(@class, 'venue-title')]/following-sibling::p"));
                        if (!locationElements.isEmpty()) {
                            String fullLocation = locationElements.get(0).getText().trim();
                            String[] result = processLocationString(fullLocation);
                            venueName = result[0];
                            location = result[1];
                        }
                    }
                }
            } catch (Exception ignored) {}
            String dateStr = startDate;
            if (!endDate.isEmpty()) dateStr = startDate + " - " + endDate;
            return new DetailedEventInfo(title, "", dateStr, venueName, location, eventUrl);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error fetching detailed event info from: " + eventUrl, e);
            return null;
        }
    }

    private LocalDate extractStartDateTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        String[] parts = dateStr.split("-");
        String startPart = parts[0].trim();
        try {
            String[] tokens = startPart.split(" ");
            if (tokens.length >= 5) {
                String day = tokens[0];
                String month = tokens[1];
                String year = tokens[2];
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);
                return LocalDate.parse(String.format("%s %s %s", day, month, year), formatter);
            } else if (tokens.length >= 4) {
                String day = tokens[0];
                String month = tokens[1];
                String year = tokens[2];
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);
                return LocalDate.parse(String.format("%s %s %s", day, month, year), formatter);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private Event createEventFromDetailedInfo(EventType eventType, EventInfo basicInfo, DetailedEventInfo detailedInfo) {
        String title = !detailedInfo.getTitle().isEmpty() ? detailedInfo.getTitle() : basicInfo.getTitle();
        String dateStr = !detailedInfo.getDateStr().isEmpty() ? detailedInfo.getDateStr() : basicInfo.getDateStr();
        String venueName = !detailedInfo.getVenueName().isEmpty() ? detailedInfo.getVenueName() : basicInfo.getVenueName();
        String location = !detailedInfo.getLocation().isEmpty() ? detailedInfo.getLocation() : basicInfo.getLocation();
        String ticketUrl = !detailedInfo.getTicketUrl().isEmpty() ? detailedInfo.getTicketUrl() : basicInfo.getTicketUrl();
        String description = detailedInfo.getDescription();
        LocalDate parsedStartDate = extractStartDateTime(dateStr);
        String date = parsedStartDate != null ? parsedStartDate.format(DateTimeFormatter.ofPattern("dd/MM/yy")) : dateStr;

        Venue venue = null;
        if (venueName != null && !venueName.isEmpty()) {
            venue = venueService.findByName(venueName);
            if (venue == null) {
                venue = new Venue();
                venue.setName(venueName);
                venue.setLocation(location);
                venue = venueService.save(venue);
            }
        }

        switch (eventType) {
            case CONCERT:
                ConcertEvent concert = EventUtils.createDetailedConcertEvent(
                    new DetailedEventInfo(title, description, date, venueName, location, ticketUrl), eventType);
                if (venue != null) concert.setVenue(venue);
                return concert;
            case THEATER:
            case STANDUP:
                PerformingArt theater = EventUtils.createDetailedTheaterEvent(
                    new DetailedEventInfo(title, description, date, venueName, location, ticketUrl), eventType);
                if (venue != null) theater.setVenue(venue);
                theater.setPerformanceType(eventType == EventType.STANDUP ? PerformanceType.STANDUP : PerformanceType.THEATER);
                return theater;
            default:
                return null;
        }
    }

    public static String extractDate(String input) {
        String part = input.contains("-") ? input.split("-")[0].trim() : input.trim();
        String[] tokens = part.split(" ");
        if (tokens.length < 3) return null;
        String day = tokens[0];
        String month = tokens[1];
        String year = tokens[2];
        String dateString = String.format("%s %s %s", day, month, year);
        Locale systemLocale = Locale.getDefault();
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", systemLocale);
        try {
            LocalDate date = LocalDate.parse(dateString, inputFormatter);
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return date.format(outputFormatter);
        } catch (DateTimeParseException e) {
            try {
                DateTimeFormatter fallbackFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
                LocalDate date = LocalDate.parse(dateString, fallbackFormatter);
                DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                return date.format(outputFormatter);
            } catch (DateTimeParseException ex) {
                return null;
            }
        }
    }


    private String[] processLocationString(String fullLocation) {
        String venueName = "";
        String location = "";
        if (fullLocation != null && !fullLocation.isEmpty()) {
            if (fullLocation.contains(",")) {
                String[] parts = fullLocation.split(",");
                venueName = parts[0].trim();
                StringBuilder locationBuilder = new StringBuilder();
                for (int i = 1; i < parts.length; i++) {
                    if (i > 1) locationBuilder.append(", ");
                    locationBuilder.append(parts[i].trim());
                }
                location = locationBuilder.toString();
            } else {
                location = fullLocation;
            }
        }
        return new String[]{venueName, location};
    }

    @Override
    public String getSourceName() {
        return "Biletino";
    }

    @Override
    public boolean isAvailable() {
        try {
            driver = WebScraperUtils.initializeBrowser(config.getChromePath(), config.getWaitTimeoutSeconds());
            driver.get(BASE_URL);
            boolean available = driver.getTitle().contains("Biletino");
            WebScraperUtils.closeBrowser(driver);
            return available;
        } catch (Exception e) {
            if (driver != null) WebScraperUtils.closeBrowser(driver);
            return false;
        }
    }

    @Override
    public String getSourceUrl() {
        return BASE_URL;
    }
}
