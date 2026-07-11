package com.asaf.whatnext.scraper.bubilet;

import java.util.List;

public record BubiletListCardDraft(
        String title,
        String ticketUrl,
        String dateText,
        String imageUrl,
        String venueName,
        String venueSlug,
        Integer minPriceTl,
        boolean multiVenue,
        List<String> venueNames
) {
    public BubiletListCardDraft(String title, String ticketUrl, String dateText, String imageUrl, String venueName) {
        this(title, ticketUrl, dateText, imageUrl, venueName, null, null, false, List.of());
    }
}
