package com.asaf.whatnext.scraper.bubilet;

import java.util.List;

/**
 * Liste veya zenginleştirme sonrası Bubilet taslak satırı.
 */
public record BubiletCardDraft(
        String title,
        String ticketUrl,
        String dateText,
        String imageUrl,
        String city,
        String venueName,
        String venueSlug,
        Integer minPriceTl,
        boolean multiVenue,
        List<String> venueNames
) {
    public BubiletCardDraft(String title, String ticketUrl, String dateText, String imageUrl, String city, String venueName) {
        this(title, ticketUrl, dateText, imageUrl, city, venueName, null, null, false, List.of());
    }
}
