package com.asaf.whatnext.scraper;

import com.asaf.whatnext.enums.AudienceType;
import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.scraper.common.TurkishDateParser;
import com.asaf.whatnext.scraper.web.HtmlFetcher;
import com.asaf.whatnext.services.ScrapeHealthService;
import com.asaf.whatnext.utils.TrClock;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Zorlu PSM'nin kendi etkinlik takvimi. Biletlerin çoğu Passo'da satılıyor ve Passo sunucu (Hetzner)
 * IP'sini 403'lüyor — bu salonun envanteri buradan gelir. Sayfa sunucuda render ediliyor; "Daha fazla"
 * düğmesi {@code POST /Activity/Search} (form: index, template=List, startDate/endDate dd.MM.yyyy)
 * ile 12'şer kart döndürür. Kartta yıl yok → en yakın gelecek yıla yuvarlanır.
 */
@Service
@Order(70)
public class ZorluPsmScraper extends BaseScraper {

    static final String BASE_URL = "https://www.zorlupsm.com";
    private static final String SEARCH_URL = BASE_URL + "/Activity/Search";
    private static final DateTimeFormatter FILTER_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final int DEFAULT_DAYS = 30;
    private static final int MAX_PAGES = 40;
    private static final Locale TR = Locale.forLanguageTag("tr");

    /** Kart etiketi → tür. Sıra önceliktir (STAND-UP + TİYATRO → STANDUP). Eşleşmeyen kart (atölye, gösterim) atlanır. */
    private static final Map<String, EventType> LABELS = new LinkedHashMap<>();
    static {
        LABELS.put("STAND-UP", EventType.STANDUP);
        LABELS.put("TİYATRO", EventType.THEATER);
        LABELS.put("MÜZİKAL", EventType.PERFORMING_ART);
        LABELS.put("BALE", EventType.PERFORMING_ART);
        LABELS.put("DANS", EventType.PERFORMING_ART);
        LABELS.put("AİLE EĞLENCESİ", EventType.PERFORMING_ART);
        LABELS.put("KONSER", EventType.CONCERT);
        LABELS.put("FESTİVAL", EventType.CONCERT);
    }

    private final ScrapeHealthService scrapeHealthService;
    private final double minParseRatio;

    public ZorluPsmScraper(ScrapeHealthService scrapeHealthService,
                           @Value("${whatnext.scrape-health.min-parse-ratio:0.8}") double minParseRatio) {
        super(ZorluPsmScraper.class);
        this.scrapeHealthService = scrapeHealthService;
        this.minParseRatio = minParseRatio;
    }

    @Override
    public List<ScrapedEventData> scrapeEvents(String city) {
        LocalDate today = TrClock.today();
        return scrapeEvents(city, today, today.plusDays(DEFAULT_DAYS));
    }

    /** Tarih aralığı siteye filtre olarak gider: tüm yıl ~300 kart ve sayfa başı 4–12 sn. */
    @Override
    public List<ScrapedEventData> scrapeEvents(String city, LocalDate startDate, LocalDate endDate) {
        if (city != null && !city.isBlank() && !"istanbul".equals(ScrapedEventData.normalizeCity(city))) {
            return List.of();
        }
        logger.info("Scraping events from Zorlu PSM " + startDate + " – " + endDate);
        ScrapeRunStats stats = new ScrapeRunStats(getSourceName());
        Map<String, ScrapedEventData> byUrl = new LinkedHashMap<>();
        LocalDate today = TrClock.today();

        for (int index = 0; index < MAX_PAGES; index++) {
            List<Element> cards;
            try {
                Document page = HtmlFetcher.html(HttpRequest.newBuilder(URI.create(SEARCH_URL))
                        .header("User-Agent", HtmlFetcher.USER_AGENT)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .timeout(Duration.ofSeconds(40))
                        .POST(HttpRequest.BodyPublishers.ofString("index=" + index + "&template=List&isFree="
                                + "&startDate=" + FILTER_DATE.format(startDate)
                                + "&endDate=" + FILTER_DATE.format(endDate)))
                        .build());
                cards = page.select("div.event-list-card");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Zorlu PSM sayfası alınamadı (index " + index + ")", e);
                break;
            }
            for (Element card : cards) {
                ScrapedEventData d = toEventData(card, today);
                if (d == null) {
                    // Etiketi tür eşlemeyen kartlar (atölye, film gösterimi, çocuk spor dersleri) bilinçli
                    // atlanır; sağlık oranını bozmasın diye sayılmaz.
                    continue;
                }
                stats.recordCardSeen();
                if (d.getStartDate() == null) {
                    stats.recordMissingDate(d.getTitle());
                    continue;
                }
                byUrl.putIfAbsent(d.getTicketUrl() + "|" + d.getStartDate() + "|" + d.getStartTime(), d);
                stats.recordParsed(d.getTitle());
            }
            if (cards.size() < 12) {
                break;
            }
        }

        List<ScrapedEventData> events = filterByDate(new ArrayList<>(byUrl.values()), startDate, endDate);
        logger.info("Scraped " + events.size() + " events from Zorlu PSM");
        finishAndRecordStats(stats, scrapeHealthService, minParseRatio);
        return events;
    }

    /** Liste kartı → {@link ScrapedEventData}; başlık/link yoksa ya da etiket tür eşlemiyorsa {@code null}. */
    static ScrapedEventData toEventData(Element card, LocalDate today) {
        Element title = card.selectFirst("h2");
        Element detail = card.selectFirst("a.event-list-card-item-detail-text[href]");
        if (title == null || detail == null || title.text().isBlank()) {
            return null;
        }
        Set<String> labels = card.select("span.btn.label").stream()
                .map(e -> e.text().trim().toUpperCase(TR))
                .collect(Collectors.toSet());
        EventType type = LABELS.entrySet().stream()
                .filter(e -> labels.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
        if (type == null) {
            return null;
        }

        Element ticket = card.selectFirst("a.btn-secondary[href^=http]");
        Element img = card.selectFirst(".event-list-card-content img[src]");
        Element date = card.selectFirst(".col-location p.date");
        Element hour = card.selectFirst(".col-location b.hour");

        return ScrapedEventData.builder()
                .title(title.text().trim())
                .rawDateString(date == null ? null : date.text() + (hour == null ? "" : " " + hour.text()))
                .startDate(date == null ? null : parseDayMonth(date.text(), today))
                .startTime(hour == null ? null : parseTime(hour.text()))
                .venueName(venueName(card))
                .location("istanbul")
                .city("istanbul")
                // Passo linki varsa onu ver (bilet orada); yoksa salonun detay sayfası.
                .ticketUrl(ticket != null ? ticket.attr("href") : BASE_URL + detail.attr("href"))
                .imageUrl(img == null ? null : BASE_URL + img.attr("src"))
                .eventType(type)
                .audience(labels.contains("ÇOCUK") ? AudienceType.CHILDREN : null)
                .source(EventSourceType.ZORLU_PSM)
                .build();
    }

    /** "ZORLU PSM" → "Zorlu PSM"; salon adı varsa "Zorlu PSM - Turkcell Sahnesi" (Passo'nun yazımı). */
    static String venueName(Element card) {
        Element place = card.selectFirst(".location.place p");
        String hall = place == null ? "" : place.text().trim();
        if (hall.isEmpty() || hall.equalsIgnoreCase("ZORLU PSM")) {
            return "Zorlu PSM";
        }
        // Salon adları İngilizce karışık (STUDIO, PLATINUM): TR küçültme "Studıo" yapar → İ düzeltilip ROOT ile.
        String pretty = Arrays.stream(hall.replace("İ", "i").toLowerCase(Locale.ROOT).split(" "))
                .map(w -> w.isEmpty() ? w : w.substring(0, 1).toUpperCase(Locale.ROOT) + w.substring(1))
                .collect(Collectors.joining(" "));
        return "Zorlu PSM - " + pretty;
    }

    /** "06 KASIM" → bu yıl; bugünden 30+ gün gerideyse gelecek yıl (kartta yıl yok). */
    static LocalDate parseDayMonth(String text, LocalDate today) {
        String[] parts = text.trim().split("\\s+");
        if (parts.length < 2) {
            return null;
        }
        String month = parts[1].toLowerCase(TR);
        int m = TurkishDateParser.monthNumberOrZero(month.substring(0, 1).toUpperCase(TR) + month.substring(1));
        try {
            LocalDate d = LocalDate.of(today.getYear(), m, Integer.parseInt(parts[0]));
            return d.isBefore(today.minusDays(30)) ? d.plusYears(1) : d;
        } catch (NumberFormatException | DateTimeException e) {
            return null;
        }
    }

    private static LocalTime parseTime(String text) {
        try {
            return LocalTime.parse(text.trim());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getSourceName() {
        return "ZorluPSM";
    }

    @Override
    public String getSourceUrl() {
        return BASE_URL;
    }

    @Override
    public EventSourceType getEventSourceType() {
        return EventSourceType.ZORLU_PSM;
    }

    @Override
    protected String getDefaultCity() {
        return "istanbul";
    }
}
