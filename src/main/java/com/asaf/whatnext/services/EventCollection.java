package com.asaf.whatnext.services;

import org.openqa.selenium.WebElement;
import java.util.List;

/**
 * Class to hold event cards and their URLs.
 */
public class EventCollection {
    private final List<WebElement> eventCards;
    private final List<String> eventUrls;

    /**
     * Constructs an EventCollection with the given event cards and URLs.
     *
     * @param eventCards List of WebElements representing event cards
     * @param eventUrls List of URLs corresponding to the event cards
     */
    public EventCollection(List<WebElement> eventCards, List<String> eventUrls) {
        this.eventCards = eventCards;
        this.eventUrls = eventUrls;
    }

    /**
     * Gets the list of event cards.
     *
     * @return List of WebElements representing event cards
     */
    public List<WebElement> getEventCards() {
        return eventCards;
    }

    /**
     * Gets the list of event URLs.
     *
     * @return List of URLs corresponding to the event cards
     */
    public List<String> getEventUrls() {
        return eventUrls;
    }
}