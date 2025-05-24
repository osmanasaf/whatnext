package com.asaf.whatnext.services;

import com.asaf.whatnext.config.BiletinoScraperConfig;
import com.asaf.whatnext.enums.Biletino.BiletinoCategory;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.enums.PerformanceType;
import com.asaf.whatnext.models.*;
import com.asaf.whatnext.service.ArtistService;
import com.asaf.whatnext.service.ConcertEventService;
import com.asaf.whatnext.service.TheaterEventService;
import com.asaf.whatnext.service.VenueService;
import com.asaf.whatnext.utils.WebScraperUtils;
import org.jsoup.nodes.Document;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private final ConcertEventService concertEventService;
    private final TheaterEventService theaterEventService;
    private final ArtistService artistService;
    private final VenueService venueService;
    private final BiletinoScraperConfig config;
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
        List<Event> allEvents = new ArrayList<>();
        try {
            allEvents.addAll(processCategory(BiletinoCategory.MUSIC));
            allEvents.addAll(processCategory(BiletinoCategory.THEATRE));
            allEvents.addAll(processCategory(BiletinoCategory.COMEDY));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching events from Biletino", e);
        }
        return allEvents;
    }

    private List<Event> processCategory(BiletinoCategory category) {
        List<Event> events = new ArrayList<>();
        try {
            initializeBrowser();
            List<Event> categoryEvents = fetchEventsByCategory(category);
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
        List<Event> events = new ArrayList<>();
        try {
            String searchUrl = config.buildSearchUrl(category);
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
                if (event != null) events.add(event);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error processing event card", e);
            }
        }
        return events;
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

    private Event createEventFromDetailedInfo(EventType eventType, EventInfo basicInfo, DetailedEventInfo detailedInfo) {
        String title = !detailedInfo.getTitle().isEmpty() ? detailedInfo.getTitle() : basicInfo.getTitle();
        String dateStr = !detailedInfo.getDateStr().isEmpty() ? detailedInfo.getDateStr() : basicInfo.getDateStr();
        String venueName = !detailedInfo.getVenueName().isEmpty() ? detailedInfo.getVenueName() : basicInfo.getVenueName();
        String location = !detailedInfo.getLocation().isEmpty() ? detailedInfo.getLocation() : basicInfo.getLocation();
        String ticketUrl = !detailedInfo.getTicketUrl().isEmpty() ? detailedInfo.getTicketUrl() : basicInfo.getTicketUrl();
        String description = detailedInfo.getDescription();
        switch (eventType) {
            case CONCERT:
                return createDetailedConcertEvent(title, description, dateStr, venueName, location, ticketUrl);
            case THEATER:
                return createDetailedTheaterEvent(title, description, dateStr, venueName, location, ticketUrl, eventType);
            case STANDUP:
                return createDetailedTheaterEvent(title, description, dateStr, venueName, location, ticketUrl, eventType);
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

    private ConcertEvent createDetailedConcertEvent(String title, String description, String dateStr,
                                                    String venueName, String location, String ticketUrl) {
        ConcertEvent concert = new ConcertEvent();
        setCommonEventProperties(concert, title, description, dateStr, ticketUrl);
        Artist artist = new Artist();
        artist.setName(extractArtistName(title));
        concert.setArtist(artist);
        if (!venueName.isEmpty() || !location.isEmpty()) {
            Venue venue = new Venue();
            venue.setName(venueName);
            venue.setLocation(location);
            concert.setVenue(venue);
        }
        return concert;
    }

    private PerformingArt createDetailedTheaterEvent(String title, String description, String dateStr,
                                                     String venueName, String location, String ticketUrl,
                                                     EventType eventType) {
        PerformingArt theater = new PerformingArt();
        setCommonEventProperties(theater, title, description, dateStr, ticketUrl);
        if (eventType == EventType.STANDUP) {
            theater.setPerformanceType(PerformanceType.STANDUP);
        } else {
            theater.setPerformanceType(PerformanceType.THEATER);
        }
        return theater;
    }

    private <T extends Event> void setCommonEventProperties(T event, String title, String description, String dateStr, String ticketUrl) {
        event.setTitle(title);
        if (description != null && !description.isEmpty()) event.setDescription(description);
        if (dateStr != null && !dateStr.isEmpty()) {
            if (dateStr.contains(" - ")) {
                String[] dates = dateStr.split(" - ");
                if (dates.length >= 2) {
                    event.setStartDate(parseDetailedDate(dates[0]));
                    LocalDate endDate = parseDetailedDate(dates[1]);
                    if (endDate != null) event.setEndDate(endDate);
                }
            } else {
                event.setStartDate(parseDetailedDate(dateStr));
            }
        }
        event.setSource(EventSourceType.BILETINO);
        if (ticketUrl != null && !ticketUrl.isEmpty()) event.setTicketUrl(ticketUrl);
    }

    private LocalDate parseDetailedDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            String[] parts = dateStr.split("\\s+");
            if (parts.length >= 3) {
                int day = Integer.parseInt(parts[0]);
                String month = parts[1];
                int year = Integer.parseInt(parts[2]);
                int monthNum = getMonthNumber(month);
                return LocalDate.of(year, monthNum, day);
            }
            return parseDate(dateStr);
        } catch (Exception e) {
            return parseDate(dateStr);
        }
    }

    private String extractArtistName(String title) {
        if (title.contains("-")) return title.split("-")[0].trim();
        return title.trim();
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return LocalDate.parse(dateStr, SEARCH_RESULT_DATE_FORMATTER);
        } catch (Exception e) {
            try {
                String[] parts = dateStr.split("\\s+");
                if (parts.length >= 2) {
                    int day = Integer.parseInt(parts[0]);
                    String month = parts[1];
                    int year = LocalDate.now().getYear();
                    int monthNum = getMonthNumber(month);
                    return LocalDate.of(year, monthNum, day);
                }
                return null;
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private int getMonthNumber(String monthName) {
        if (monthName == null || monthName.isEmpty()) return 1;
        String lowerMonth = monthName.toLowerCase();
        switch (lowerMonth) {
            case "jan": case "january": case "ocak": case "oca": return 1;
            case "feb": case "february": case "şubat": case "şub": return 2;
            case "mar": case "march": case "mart": return 3;
            case "apr": case "april": case "nisan": case "nis": return 4;
            case "may": case "mayıs": return 5;
            case "jun": case "june": case "haziran": case "haz": return 6;
            case "jul": case "july": case "temmuz": case "tem": return 7;
            case "aug": case "august": case "ağustos": case "ağu": return 8;
            case "sep": case "september": case "eylül": case "eyl": return 9;
            case "oct": case "october": case "ekim": case "eki": return 10;
            case "nov": case "november": case "kasım": case "kas": return 11;
            case "dec": case "december": case "aralık": case "ara": return 12;
            default: return 1;
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
