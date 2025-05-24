package com.asaf.whatnext.services;

/**
 * Container class for detailed event information extracted from an event page.
 */
public class DetailedEventInfo {
    private final String title;
    private final String description;
    private final String dateStr;
    private final String venueName;
    private final String location;
    private final String ticketUrl;

    /**
     * Constructs a DetailedEventInfo with the given detailed event information.
     *
     * @param title The title of the event
     * @param description The description of the event
     * @param dateStr The date string of the event
     * @param venueName The venue name of the event
     * @param location The location of the event
     * @param ticketUrl The ticket URL of the event
     */
    public DetailedEventInfo(String title, String description, String dateStr, String venueName, String location, String ticketUrl) {
        this.title = title;
        this.description = description;
        this.dateStr = dateStr;
        this.venueName = venueName;
        this.location = location;
        this.ticketUrl = ticketUrl;
    }

    /**
     * Gets the title of the event.
     *
     * @return The event title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the description of the event.
     *
     * @return The event description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the date string of the event.
     *
     * @return The event date string
     */
    public String getDateStr() {
        return dateStr;
    }

    /**
     * Gets the venue name of the event.
     *
     * @return The event venue name
     */
    public String getVenueName() {
        return venueName;
    }

    /**
     * Gets the location of the event.
     *
     * @return The event location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Gets the ticket URL of the event.
     *
     * @return The event ticket URL
     */
    public String getTicketUrl() {
        return ticketUrl;
    }
}
