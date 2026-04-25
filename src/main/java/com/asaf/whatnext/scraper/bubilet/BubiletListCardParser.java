package com.asaf.whatnext.scraper.bubilet;

import com.asaf.whatnext.config.BubiletScraperConfig;
import com.asaf.whatnext.scraper.common.ElementTextExtractor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Optional;

public class BubiletListCardParser {

    public Optional<BubiletListCardDraft> parse(String cardHtml) {
        if (cardHtml == null || cardHtml.isBlank()) {
            return Optional.empty();
        }

        Document doc = Jsoup.parse(cardHtml, BubiletScraperConfig.BASE_URL);
        Element link = firstNonNull(doc.selectFirst("a[href*='/etkinlik/']"), doc.selectFirst("[href*='/etkinlik/']"));
        if (link == null) {
            return Optional.empty();
        }

        String title = text(link.selectFirst(BubiletScraperConfig.EVENT_TITLE_SELECTOR));
        String ticketUrl = link.absUrl("href");
        if (ticketUrl.isBlank()) {
            ticketUrl = normalizeUrl(link.attr("href"));
        }
        if (title.isBlank() || ticketUrl.equals(BubiletScraperConfig.BASE_URL)) {
            return Optional.empty();
        }

        VenueDate venueDate = extractVenueAndDate(link);
        Element image = link.selectFirst(BubiletScraperConfig.EVENT_IMAGE_SELECTOR);
        String imageUrl = image == null ? "" : firstNonBlank(image.absUrl("src"), image.attr("src"));

        return Optional.of(new BubiletListCardDraft(
                title,
                ticketUrl,
                venueDate.dateText(),
                imageUrl,
                venueDate.venueName()));
    }

    private VenueDate extractVenueAndDate(Element card) {
        Element textBlock = card.selectFirst(BubiletScraperConfig.EVENT_TEXT_BLOCK);
        if (textBlock != null) {
            Elements lines = textBlock.select("p.text-gray-500");
            if (lines.size() >= 2) {
                return new VenueDate(text(lines.get(0)), text(lines.get(1)));
            }
            if (lines.size() == 1) {
                String one = text(lines.get(0));
                if (BubiletDateRules.looksLikeListDateLine(one)) {
                    return new VenueDate("", one);
                }
                return new VenueDate(one, "");
            }
        }

        String venue = text(card.selectFirst(BubiletScraperConfig.EVENT_VENUE_AFTER_H3));
        String date = text(card.selectFirst(BubiletScraperConfig.EVENT_DATE_AFTER_H3));
        if (!venue.isBlank() || !date.isBlank()) {
            return new VenueDate(venue, date);
        }

        return new VenueDate(
                ElementTextExtractor.firstNonBlankFromChildren(card, BubiletScraperConfig.EVENT_VENUE_ALTERNATE_SELECTORS),
                text(card.selectFirst(BubiletScraperConfig.EVENT_DATE_SELECTOR)));
    }

    private String normalizeUrl(String href) {
        if (href == null || href.isBlank()) {
            return "";
        }
        if (href.startsWith("http")) {
            return href;
        }
        return BubiletScraperConfig.BASE_URL + href;
    }

    private static String text(Element element) {
        return element == null ? "" : element.text().trim();
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second == null ? "" : second.trim();
    }

    private static <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private record VenueDate(String venueName, String dateText) {
    }
}
