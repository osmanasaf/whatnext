package com.asaf.whatnext.scraper.bubilet;

public record BubiletListCardDraft(
        String title,
        String ticketUrl,
        String dateText,
        String imageUrl,
        String venueName
) {
}
