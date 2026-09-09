package com.asaf.whatnext.scraper.biletinial;

import com.asaf.whatnext.scraper.ScrapedEventData;
import com.asaf.whatnext.utils.Texts;
import com.asaf.whatnext.scraper.web.HtmlFetcher;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Biletinial detay (schema.org seans) okuma; sayfadaki mekân metni açık sürücüde okunur.
 */
public class BiletinialDetailParser {

    private static final String DETAIL_TICKET_SECTION_ID = "info_ticket";
    private static final String DETAIL_CITY_BLOCK = ".ed-biletler__sehir[data-sehir]";
    private static final String DETAIL_SESSION_BLOCK = ".ed-biletler__sehir__gun";
    private static final String DETAIL_START_DATE = "time[itemprop='startDate']";
    private static final String DETAIL_VENUE_ADDRESS = "address[itemprop='name']";

    private static final Logger LOG = LoggerFactory.getLogger(BiletinialDetailParser.class);

    /**
     * @param events           detay seans eşleşmeleri (boş olabilir)
     * @param venueTextFromPage detayda okunan yedek mekân; liste-only birleşimde kullanılır
     */
    public record ParseResult(List<ScrapedEventData> events, String venueTextFromPage) {
    }

    public ParseResult loadSessionsAndVenue(BiletinialCardDraft d, int timeoutMs) {
        List<ScrapedEventData> out = new ArrayList<>();
        String venueTextFromPage = "";
        try {
            Document doc = HtmlFetcher.getOrNull(d.ticketUrl(), timeoutMs);
            if (doc == null || doc.getElementById(DETAIL_TICKET_SECTION_ID) == null) {
                return new ParseResult(out, venueTextFromPage);
            }

            venueTextFromPage = readVenueText(doc);
            String baseVenue = Texts.firstNonBlank(d.venueName(), venueTextFromPage);

            List<Element> cityBlocks = doc.select(DETAIL_CITY_BLOCK);
            Set<String> seenSessionKeys = new LinkedHashSet<>();

            for (Element block : cityBlocks) {
                String dataSehir = block.attr("data-sehir");
                if (!regionMatchesListSlug(dataSehir, d.cityValue())) {
                    continue;
                }

                List<Element> sessions = block.select(DETAIL_SESSION_BLOCK);
                for (Element session : sessions) {
                    List<Element> times = session.select(DETAIL_START_DATE);
                    if (times.isEmpty()) {
                        continue;
                    }
                    String content = times.get(0).attr("content");
                    if (content == null || content.isBlank()) {
                        continue;
                    }
                    LocalDateTime ldt;
                    try {
                        ldt = LocalDateTime.parse(content);
                    } catch (Exception e) {
                        LOG.debug("Biletinial detay startDate parse edilemedi: {}", content);
                        continue;
                    }
                    if (!ldt.toLocalDate().equals(d.listDate())) {
                        continue;
                    }

                    String venue = baseVenue;
                    Element addr = session.selectFirst(DETAIL_VENUE_ADDRESS);
                    if (addr != null && !addr.text().trim().isEmpty()) {
                        venue = addr.text().trim();
                    }

                    String raw = times.get(0).text();
                    if (raw == null || raw.isBlank()) {
                        raw = ldt.toString();
                    }

                    String key = d.ticketUrl() + "|" + content;
                    if (!seenSessionKeys.add(key)) {
                        continue;
                    }

                    out.add(BiletinialEventMapper.fromDetailSession(
                            d, ldt.toLocalDate(), ldt.toLocalTime(), raw, venue));
                }
            }
        } catch (Exception e) {
            LOG.debug("Biletinial detay okunamadı: {} — {}", d.ticketUrl(), e.getMessage());
        }
        return new ParseResult(out, venueTextFromPage);
    }

    private static String readVenueText(Document doc) {
        for (Element el : doc.select(DETAIL_VENUE_ADDRESS)) {
            if (!el.text().isBlank()) {
                return el.text().trim();
            }
        }
        return "";
    }

    private static boolean regionMatchesListSlug(String dataSehir, String citySlug) {
        if (dataSehir == null || citySlug == null) {
            return false;
        }
        String dn = normalizeForMatch(dataSehir);
        String slug = citySlug.trim().toLowerCase(Locale.ROOT);
        if (slug.equals("istanbul-anadolu")) {
            return dn.contains("anadolu");
        }
        if (slug.equals("istanbul-avrupa")) {
            return dn.contains("avrupa");
        }
        return dn.contains(normalizeForMatch(slug.replace('-', ' ')))
                || dn.contains(slug.replace("-", ""));
    }

    private static String normalizeForMatch(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().toLowerCase(Locale.forLanguageTag("tr"))
                .replace('ı', 'i')
                .replace(" ", "");
    }
}
