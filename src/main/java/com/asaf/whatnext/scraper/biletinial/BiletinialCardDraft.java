package com.asaf.whatnext.scraper.biletinial;

import com.asaf.whatnext.enums.Biletinial.BiletinialCategory;

import java.time.LocalDate;

public record BiletinialCardDraft(
        String title,
        String ticketUrl,
        String imageUrl,
        String venueName,
        String cityLabel,
        BiletinialCategory category,
        String cityValue,
        LocalDate listDate
) {
}
