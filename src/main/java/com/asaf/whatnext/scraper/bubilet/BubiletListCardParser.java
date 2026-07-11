package com.asaf.whatnext.scraper.bubilet;

import com.asaf.whatnext.config.BubiletScraperConfig;
import com.asaf.whatnext.scraper.common.ElementTextExtractor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class BubiletListCardParser {

    public Optional<BubiletListCardDraft> parse(String cardHtml) {
        if (cardHtml == null || cardHtml.isBlank()) {
            return Optional.empty();
        }

        Document doc = Jsoup.parse(cardHtml, BubiletScraperConfig.BASE_URL);
        Element root = doc.body().childrenSize() > 0 ? doc.body().child(0) : doc.body();

        Element h3 = root.selectFirst(BubiletScraperConfig.EVENT_TITLE_SELECTOR);
        if (h3 == null) {
            return Optional.empty();
        }
        String title = text(h3);
        if (title.isBlank()) {
            return Optional.empty();
        }

        Element datePriceTrigger = root.selectFirst(BubiletScraperConfig.EVENT_DATEPRICE_TRIGGER);
        if (datePriceTrigger != null) {
            return Optional.of(parseNewGrid(root, h3, title, datePriceTrigger));
        }

        return parseLegacyGrid(root, h3, title);
    }

    /**
     * 2026 ızgara: başlık/mekân/tarih+fiyat üç ayrı {@code <a>}'ya bölünmüş.
     */
    private BubiletListCardDraft parseNewGrid(Element root, Element h3, String title, Element datePriceTrigger) {
        Element titleAnchor = "a".equalsIgnoreCase(h3.parent() != null ? h3.parent().tagName() : "") ? h3.parent() : null;
        String ticketUrl = titleAnchor != null ? firstNonBlank(titleAnchor.absUrl("href"), normalizeUrl(titleAnchor.attr("href"))) : "";
        if (ticketUrl.isBlank()) {
            ticketUrl = firstNonBlank(datePriceTrigger.absUrl("href"), normalizeUrl(datePriceTrigger.attr("href")));
        }

        Element image = root.selectFirst(BubiletScraperConfig.EVENT_IMAGE_SELECTOR);
        String imageUrl = image == null ? "" : firstNonBlank(image.absUrl("src"), image.attr("src"));

        Element venueAnchor = root.selectFirst(BubiletScraperConfig.EVENT_VENUE_ANCHOR);
        String venueName;
        String venueSlug = null;
        boolean multiVenue = false;
        List<String> venueNames = List.of();
        if (venueAnchor != null) {
            venueName = text(venueAnchor);
            venueSlug = slugFromMekanHref(venueAnchor.attr("href"));
            venueNames = List.of(venueName);
        } else {
            Element dateContainer = datePriceTrigger.parent() != null ? datePriceTrigger.parent() : root;
            String venueLine = directChildVenueLine(dateContainer, datePriceTrigger, titleAnchor);
            if (!venueLine.isBlank() && BubiletDateRules.looksLikeVenueList(venueLine)) {
                multiVenue = true;
                venueNames = Arrays.stream(venueLine.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .toList();
                venueName = "";
            } else {
                venueName = "";
            }
        }

        Element dateP = datePriceTrigger.selectFirst("p");
        String dateText = text(dateP);

        Element priceSpan = datePriceTrigger.selectFirst(BubiletScraperConfig.EVENT_PRICE_VALUE_SELECTOR);
        Integer minPriceTl = parsePrice(priceSpan == null ? null : priceSpan.text());

        return new BubiletListCardDraft(title, ticketUrl, dateText, imageUrl, venueName, venueSlug, minPriceTl, multiVenue, venueNames);
    }

    /**
     * Venue anchor'ı yok: mekân satırı, kartın doğrudan çocuğu bir {@code <p>} olarak durur
     * (başlık anchor'ı ile date-price-trigger anchor'ı arasında).
     */
    private String directChildVenueLine(Element root, Element datePriceTrigger, Element titleAnchor) {
        for (Element child : root.children()) {
            if (child == datePriceTrigger || child == titleAnchor) {
                continue;
            }
            if ("p".equalsIgnoreCase(child.tagName())) {
                String t = text(child);
                if (!t.isBlank()) {
                    return t;
                }
            }
        }
        return "";
    }

    private static String slugFromMekanHref(String href) {
        if (href == null || href.isBlank()) {
            return null;
        }
        int i = href.indexOf("/mekan/");
        if (i < 0) {
            return null;
        }
        String slug = href.substring(i + "/mekan/".length());
        int q = slug.indexOf('?');
        if (q >= 0) {
            slug = slug.substring(0, q);
        }
        int slash = slug.indexOf('/');
        if (slash >= 0) {
            slug = slug.substring(0, slash);
        }
        return slug.isBlank() ? null : slug;
    }

    private static Integer parsePrice(String priceText) {
        if (priceText == null || priceText.isBlank()) {
            return null;
        }
        String digits = priceText.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Eski ızgara (2025 öncesi, canlıda ölü) — tek {@code <a>} içinde h3 + iki {@code <p>} kardeşi.
     * Sadece fallback/regresyon; mevcut testler bu yolu kullanır.
     */
    private Optional<BubiletListCardDraft> parseLegacyGrid(Element root, Element h3, String title) {
        Element link = "a".equalsIgnoreCase(root.tagName()) ? root : root.selectFirst("a[href*='/etkinlik/']");
        if (link == null) {
            link = root.selectFirst("[href*='/etkinlik/']");
        }
        String ticketUrl = link == null ? "" : firstNonBlank(link.absUrl("href"), normalizeUrl(link.attr("href")));
        if (ticketUrl.isBlank() || ticketUrl.equals(BubiletScraperConfig.BASE_URL)) {
            return Optional.empty();
        }

        VenueDate venueDate = extractVenueAndDateLegacy(root);
        Element image = root.selectFirst(BubiletScraperConfig.EVENT_IMAGE_SELECTOR);
        String imageUrl = image == null ? "" : firstNonBlank(image.absUrl("src"), image.attr("src"));

        return Optional.of(new BubiletListCardDraft(
                title,
                ticketUrl,
                venueDate.dateText(),
                imageUrl,
                venueDate.venueName()));
    }

    private VenueDate extractVenueAndDateLegacy(Element card) {
        VenueDate fromH3 = fromH3AndSiblingParagraphs(card);
        if (fromH3 != null) {
            return fromH3;
        }

        Element textBlock = card.selectFirst(BubiletScraperConfig.EVENT_TEXT_BLOCK);
        if (textBlock != null) {
            Elements lines = textBlock.select("p.text-gray-500");
            if (lines.isEmpty()) {
                lines = textBlock.select("> p, .min-w-0 > p, div > p");
            }
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

    /**
     * 2025+ ızgara: {@code h3} başlık, ardından aynı kapsayıcıda doğrudan {@code p} satırları
     * (mekân, sonra tarih). Sadece {@code p.text-gray-500} ile sınırlamak bazı sürümlerde satırı kaçırıyor.
     */
    private static VenueDate fromH3AndSiblingParagraphs(Element card) {
        Element h3 = card.selectFirst("h3");
        if (h3 == null) {
            return null;
        }
        Element parent = h3.parent();
        if (parent == null) {
            return null;
        }
        List<String> afterH3P = new ArrayList<>();
        boolean after = false;
        for (Element child : parent.children()) {
            if (child == h3) {
                after = true;
                continue;
            }
            if (after) {
                if ("p".equalsIgnoreCase(child.tagName())) {
                    String t = text(child);
                    if (!t.isEmpty()) {
                        afterH3P.add(t);
                    }
                } else {
                    if (!afterH3P.isEmpty()) {
                        break;
                    }
                }
            }
        }
        if (afterH3P.isEmpty() && h3.nextElementSibling() != null) {
            Element sib = h3.nextElementSibling();
            while (sib != null && "p".equalsIgnoreCase(sib.tagName())) {
                String t = text(sib);
                if (!t.isEmpty()) {
                    afterH3P.add(t);
                }
                sib = sib.nextElementSibling();
            }
        }
        if (afterH3P.size() >= 2) {
            String a = afterH3P.get(0);
            String b = afterH3P.get(1);
            boolean aDate = BubiletDateRules.looksLikeListDateLine(a);
            boolean bDate = BubiletDateRules.looksLikeListDateLine(b);
            if (aDate && !bDate) {
                return new VenueDate(b, a);
            }
            return new VenueDate(a, b);
        }
        if (afterH3P.size() == 1) {
            String one = afterH3P.get(0);
            if (BubiletDateRules.looksLikeListDateLine(one)) {
                return new VenueDate("", one);
            }
            return new VenueDate(one, "");
        }
        return null;
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

    private record VenueDate(String venueName, String dateText) {
    }
}
