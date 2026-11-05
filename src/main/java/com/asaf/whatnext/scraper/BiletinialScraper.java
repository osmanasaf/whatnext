package com.asaf.whatnext.scraper;

import com.asaf.whatnext.config.BiletinialScraperConfig;
import com.asaf.whatnext.enums.Biletinial.BiletinialCategory;
import com.asaf.whatnext.enums.Biletinial.BiletinialCity;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.scraper.biletinial.BiletinialAddressVenueParser;
import com.asaf.whatnext.scraper.biletinial.BiletinialCardDraft;
import com.asaf.whatnext.scraper.biletinial.BiletinialDetailParser;
import com.asaf.whatnext.scraper.biletinial.BiletinialEventMapper;
import com.asaf.whatnext.services.ScrapeHealthService;
import com.asaf.whatnext.utils.Texts;
import com.asaf.whatnext.scraper.web.HtmlFetcher;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Order(20)
public class BiletinialScraper extends BaseScraper {

    private static final String BASE_URL = "https://biletinial.com";
    private static final String SEARCH_URL_TEMPLATE = "%s/tr-tr/%s/%s?date=%s&enddate=%s";
    private static final String EVENT_CARD_SELECTOR = "#kategori__etkinlikler li:has(h3)"; // 2026-10 hpv2 kartı içinde etiket <li>Tiyatro</li> da var
    private static final String TITLE_SELECTOR = "h3 a";
    private static final String LOCATION_SELECTOR = "address";
    private static final String IMAGE_SELECTOR = "img[src*='merlincdn.net']";

    private final int timeoutMs;
    private final int scrapeDateRangeDays;
    private final ScrapeHealthService scrapeHealthService;
    private final double minParseRatio;

    @Autowired
    public BiletinialScraper(
            BiletinialScraperConfig config,
            ScrapeHealthService scrapeHealthService,
            @Value("${biletinial.scrape.date-range-days:14}") int scrapeDateRangeDays,
            @Value("${whatnext.scrape-health.min-parse-ratio:0.8}") double minParseRatio) {
        super(BiletinialScraper.class);
        this.timeoutMs = config.getTimeoutMs();
        this.scrapeDateRangeDays = scrapeDateRangeDays;
        this.scrapeHealthService = scrapeHealthService;
        this.minParseRatio = minParseRatio;
    }

    @Override
    public List<ScrapedEventData> scrapeEvents(String city) {
        logger.info("Scraping events from Biletinial for city: " + city);

        BiletinialCity selectedCity = parseCity(city);
        ScrapeRunStats stats = new ScrapeRunStats(getSourceName());

        // İstanbul sitede iki bölge slug'ına ayrılmış; diğer şehirler tek slug.
        List<String> citySlugs = selectedCity == BiletinialCity.ISTANBUL
                ? List.of("istanbul-anadolu", "istanbul-avrupa")
                : List.of(selectedCity.getValue());

        List<ScrapedEventData> allData = new ArrayList<>();
        for (String citySlug : citySlugs) {
            for (BiletinialCategory category : Arrays.asList(
                    BiletinialCategory.THEATRE, BiletinialCategory.MUSIC, BiletinialCategory.STANDUP)) {
                allData.addAll(scrapeCategory(category, citySlug, stats));
            }
        }
        logger.info("Scraped " + allData.size() + " events from Biletinial");

        long validCount = allData.stream().filter(ScrapedEventData::isValid).count();
        logger.info("Valid events: " + validCount + "/" + allData.size());

        finishAndRecordStats(stats, scrapeHealthService, minParseRatio);

        return allData;
    }

    private List<ScrapedEventData> scrapeCategory(BiletinialCategory category, String cityValue, ScrapeRunStats stats) {
        List<ScrapedEventData> events = new ArrayList<>();
        LocalDate today = com.asaf.whatnext.utils.TrClock.today();

        for (int i = 0; i < scrapeDateRangeDays; i++) {
            LocalDate date = today.plusDays(i);
            String dateStr = date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

            String searchUrl = String.format(SEARCH_URL_TEMPLATE, BASE_URL, category.getValue(), cityValue, dateStr, dateStr);

            try {
                List<Element> cards = HtmlFetcher.get(searchUrl, timeoutMs).select(EVENT_CARD_SELECTOR);

                BiletinialDetailParser detailParser = new BiletinialDetailParser();
                for (Element card : cards) {
                    stats.recordCardSeen();
                    try {
                        BiletinialCardDraft draft = extractCardDraft(card, category, cityValue, date);
                        if (draft == null) {
                            stats.recordParseFailed(safeSnippet(card));
                            continue;
                        }
                        if (draft.venueName() == null || draft.venueName().isBlank()) {
                            stats.recordMissingVenue(draft.title());
                        }
                        BiletinialDetailParser.ParseResult detailResult =
                                detailParser.loadSessionsAndVenue(draft, timeoutMs);
                        if (!detailResult.events().isEmpty()) {
                            events.addAll(detailResult.events());
                            stats.recordParsed(draft.title());
                        } else {
                            BiletinialCardDraft merged = mergeWithVenueHint(
                                    draft, detailResult.venueTextFromPage());
                            ScrapedEventData fallback = BiletinialEventMapper.fromListOnly(merged);
                            if (fallback != null && fallback.isValid()) {
                                events.add(fallback);
                                stats.recordParsed(draft.title());
                            } else {
                                stats.recordMissingDate(draft.title());
                            }
                        }
                    } catch (Exception e) {
                        stats.recordParseFailed(safeSnippet(card));
                        logger.debug("Error extracting card", e);
                    }
                }

            } catch (Exception e) {
                logger.warn("Error scraping date: " + date, e);
            }
        }

        return events;
    }

    private static String safeSnippet(Element card) {
        String html = card.outerHtml();
        return html.substring(0, Math.min(120, html.length()));
    }

    private BiletinialCardDraft extractCardDraft(
            Element card, BiletinialCategory category, String cityValue, LocalDate listDate) {
        try {
            Element titleAnchor = card.selectFirst(TITLE_SELECTOR);
            Element addressElement = card.selectFirst(LOCATION_SELECTOR);
            if (titleAnchor == null || addressElement == null) {
                return null;
            }
            String title = titleAnchor.text();

            Element cityElement = addressElement.selectFirst("b");
            String city = cityElement == null ? "" : cityElement.text();
            if (city != null && city.toLowerCase().contains("stanbul")) {
                city = "istanbul";
            }
            String venueName = BiletinialAddressVenueParser.extractVenueNameFromAddressHtml(
                    addressElement.outerHtml());
            String href = titleAnchor.attr("href");
            if (href == null || href.isEmpty()) {
                return null;
            }
            String ticketUrl = href.startsWith("http") ? href : BASE_URL + href;

            Element image = card.selectFirst(IMAGE_SELECTOR);
            String imageUrl = image == null ? null : image.attr("src");

            if (title.isEmpty()) {
                return null;
            }

            return new BiletinialCardDraft(
                    title.trim(), ticketUrl, imageUrl, venueName, city, category, cityValue, listDate);
        } catch (Exception e) {
            return null;
        }
    }

    private BiletinialCardDraft mergeWithVenueHint(BiletinialCardDraft d, String pageVenue) {
        String m = Texts.firstNonBlank(d.venueName(), pageVenue);
        return new BiletinialCardDraft(
                d.title(), d.ticketUrl(), d.imageUrl(), m, d.cityLabel(), d.category(), d.cityValue(), d.listDate());
    }

    private BiletinialCity parseCity(String city) {
        return Arrays.stream(BiletinialCity.values())
                .filter(c -> c.getValue().equalsIgnoreCase(city))
                .findFirst()
                .orElse(BiletinialCity.ISTANBUL);
    }

    @Override
    public String getSourceName() {
        return "Biletinial";
    }

    @Override
    public String getSourceUrl() {
        return BASE_URL;
    }

    @Override
    public EventSourceType getEventSourceType() {
        return EventSourceType.BILETINIAL;
    }

    @Override
    protected String getDefaultCity() {
        return BiletinialCity.ISTANBUL.getValue();
    }
}
