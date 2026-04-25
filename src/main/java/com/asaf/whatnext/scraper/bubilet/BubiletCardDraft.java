package com.asaf.whatnext.scraper.bubilet;

/**
 * Liste veya zenginleştirme sonrası Bubilet taslak satırı.
 */
public record BubiletCardDraft(
        String title,
        String ticketUrl,
        String dateText,
        String imageUrl,
        String city,
        String venueName
) {}
