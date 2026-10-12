package com.asaf.whatnext.services.images;

import com.asaf.whatnext.dto.EventSummaryDTO;
import com.asaf.whatnext.services.ImageGenerationService;
import com.asaf.whatnext.services.templates.GeneratedTemplateProvider;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * "remotion" motoru — {@code npx remotion still} ile React/CSS layout'tan tek kare PNG. Tasarımı
 * koda taşır: gerçek flexbox layout motoru sayesinde metin sığmama riski kalmaz (ImageMagick'in
 * Faz 3.4'teki iteratif küçültme algoritmasına ihtiyaç yok). Arka plan iki kademeli: etkinliklerden
 * birinin gerçek afişi varsa TSX tarafında kullanılır, yoksa 12 aylık kod-içi palet (bu Java tarafı
 * {@link GeneratedTemplateProvider#paletteHex} ile tek doğruluk kaynağından okur).
 *
 * Etkinlik seçimi {@link ImageGenerationService}'teki paylaşılan metotlarla birebir aynı (Faz 5.0'da
 * oraya çıkarıldı) — hangi motor seçilirse seçilsin aynı etkinlikler gösterilir, sadece render farklı.
 */
@Component
public class RemotionStillEngine implements ImageRenderEngine {

    private static final Locale TR = Locale.forLanguageTag("tr");
    private static final DateTimeFormatter DAY_MONTH_TR = DateTimeFormatter.ofPattern("d MMMM", TR);

    private final RemotionRunner remotionRunner;
    private final ImageGenerationService imageGenerationService;
    private final GeneratedTemplateProvider paletteSource;

    public RemotionStillEngine(RemotionRunner remotionRunner, ImageGenerationService imageGenerationService,
            GeneratedTemplateProvider paletteSource) {
        this.remotionRunner = remotionRunner;
        this.imageGenerationService = imageGenerationService;
        this.paletteSource = paletteSource;
    }

    @Override
    public File render(ImageRenderRequest request) throws IOException, InterruptedException {
        return render(request, null);
    }

    /**
     * {@code backgroundImageUrl} — {@link AiBackgroundEngine} kendi ürettiği arka planı buradan
     * geçirir (staticFile-göreli yol, örn. {@code "ai-backgrounds/july-post.png"}); {@code null}
     * ise TSX tarafı gerçek etkinlik afişine, o da yoksa ay paletine düşer.
     */
    public File render(ImageRenderRequest request, String backgroundImageUrl) throws IOException, InterruptedException {
        String monthName = request.date().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();
        Map<String, String> palette = paletteSource.paletteHex(monthName);

        // Hafta sonu karuselinin son iki slaytı (date = Cuma).
        if ("picks".equals(request.eventType())) {
            return renderPicks(request);
        }
        if ("cta".equals(request.eventType())) {
            return remotionRunner.run("still", "CtaStill", Map.of("date", request.date().toString()), "png");
        }
        return switch (request.postType()) {
            case "cover" -> renderCover(request, monthName, palette, backgroundImageUrl);
            case "story" -> renderStory(request, monthName, palette, backgroundImageUrl);
            default -> renderPost(request, monthName, palette, backgroundImageUrl);
        };
    }

    private File renderPost(ImageRenderRequest request, String monthName, Map<String, String> palette,
            String backgroundImageUrl) throws IOException, InterruptedException {
        // "mixed" = hafta sonu karuselinin gün slaytı (4 konser + 4 tiyatro + 2 stand-up, saat sırası).
        List<EventSummaryDTO> events = "mixed".equals(request.eventType())
                ? imageGenerationService.selectWeekendDayEvents(request.date())
                : imageGenerationService.selectPostEvents(request.eventType(), request.city(), request.date());
        if (events.isEmpty()) {
            return null;
        }
        Map<String, Object> props = new HashMap<>();
        props.put("eventType", request.eventType());
        if ("mixed".equals(request.eventType())) {
            props.put("moreCount", Math.max(0,
                    imageGenerationService.countIstanbulEventsForDate(request.date()) - events.size()));
        }
        props.put("monthName", monthName);
        props.put("palette", palette);
        props.put("date", request.date().toString());
        props.put("events", toEventMaps(events));
        putIfPresent(props, "backgroundImageUrl", backgroundImageUrl);
        return remotionRunner.run("still", "DailyPostStill", props, "png");
    }

    private File renderStory(ImageRenderRequest request, String monthName, Map<String, String> palette,
            String backgroundImageUrl) throws IOException, InterruptedException {
        List<EventSummaryDTO> events = imageGenerationService.selectStoryEvents(request.date());
        if (events.isEmpty()) {
            return null;
        }
        Map<String, Object> props = new HashMap<>();
        props.put("monthName", monthName);
        props.put("palette", palette);
        props.put("date", request.date().toString());
        props.put("events", toEventMaps(events));
        props.put("moreCount", Math.max(0, imageGenerationService.countIstanbulEventsForDate(request.date()) - events.size()));
        putIfPresent(props, "backgroundImageUrl", backgroundImageUrl);
        return remotionRunner.run("still", "SmartStoryStill", props, "png");
    }

    private File renderCover(ImageRenderRequest request, String monthName, Map<String, String> palette,
            String backgroundImageUrl) throws IOException, InterruptedException {
        // "week" (date = Pazartesi, 7 gün) / "weekend" (date = Cuma, 3 gün): karuselin açılış kapağı.
        int days = "week".equals(request.eventType()) ? 7 : "weekend".equals(request.eventType()) ? 3 : 1;
        int eventCount = 0;
        List<List<EventSummaryDTO>> perDay = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = request.date().plusDays(i);
            eventCount += imageGenerationService.countIstanbulEventsForDate(d);
            // Kapak mozaiği = günün en iyi afişleri (puan sırası; story saat sırasında, o yüzden ayrı metot).
            perDay.add(imageGenerationService.selectTopEvents(d));
        }
        // Çok günlü kapakta mozaik ilk güne yığılmasın: günlerin 1.'leri, sonra 2.'leri...
        List<EventSummaryDTO> topEvents = new ArrayList<>();
        for (int rank = 0; rank < 9; rank++) {
            for (List<EventSummaryDTO> day : perDay) {
                if (day.size() > rank) {
                    topEvents.add(day.get(rank));
                }
            }
        }
        Map<String, Object> props = new HashMap<>();
        if ("today".equals(request.eventType())) {
            props.put("kicker", "BUGÜN İSTANBUL'DA");
        }
        if (days > 1) {
            LocalDate end = request.date().plusDays(days - 1);
            String range = request.date().format(DAY_MONTH_TR) + " – " + end.format(DAY_MONTH_TR);
            if (days == 7) {
                props.put("kicker", "BU HAFTA İSTANBUL'DA");
                props.put("headline", range);
            } else {
                props.put("kicker", range.toUpperCase(TR));
                props.put("headline", "Bu hafta sonu ne yapsak?");
            }
        }
        props.put("monthName", monthName);
        props.put("palette", palette);
        props.put("date", request.date().toString());
        props.put("eventCount", eventCount);
        props.put("events", toEventMaps(topEvents));
        putIfPresent(props, "backgroundImageUrl", backgroundImageUrl);
        return remotionRunner.run("still", "CoverStill", props, "png");
    }

    /**
     * "Kaçırma · Cuma": o günün afişli en iyi 4 etkinliği, büyük afişle. Perşembe karuselinde gün başına bir slayt;
     * günün tam listesi o sabah 09:00 gönderisinde (2026-10-12 — eskiden karusel tam listeleri de taşıyordu).
     * Sıra puan sırası ({@code selectTopEvents}); DTO'daki popularityScore prod'da boş gelir.
     */
    private File renderPicks(ImageRenderRequest request) throws IOException, InterruptedException {
        List<EventSummaryDTO> picks = imageGenerationService.selectTopEvents(request.date()).stream()
                .filter(e -> e.getImageUrl() != null && !e.getImageUrl().isBlank())
                .limit(4)
                .toList();
        if (picks.size() < 2) {
            return null;
        }
        String dayName = request.date().getDayOfWeek().getDisplayName(TextStyle.FULL, TR);
        Map<String, Object> props = new HashMap<>();
        props.put("date", request.date().toString());
        props.put("kicker", ("Kaçırma • " + request.date().format(DAY_MONTH_TR)).toUpperCase(TR));
        props.put("headline", dayName.substring(0, 1).toUpperCase(TR) + dayName.substring(1));
        props.put("events", toEventMaps(picks));
        return remotionRunner.run("still", "PicksStill", props, "png");
    }

    private static void putIfPresent(Map<String, Object> props, String key, String value) {
        if (value != null && !value.isBlank()) {
            props.put(key, value);
        }
    }

    private static List<Map<String, Object>> toEventMaps(List<EventSummaryDTO> events) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (EventSummaryDTO e : events) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", e.getId() != null ? e.getId() : 0);
            m.put("title", e.getTitle() != null ? e.getTitle() : "");
            m.put("venueName", e.getVenueName() != null ? e.getVenueName() : "");
            m.put("startTime", e.getStartTime() != null ? e.getStartTime().toString() : "");
            m.put("imageUrl", e.getImageUrl() != null ? e.getImageUrl() : "");
            m.put("eventType", toStillEventType(e));
            out.add(m);
        }
        return out;
    }

    /** DTO'nun (type, performanceType) çiftini still'lerin "concert|theater|standup" etiketine indirger. */
    static String toStillEventType(EventSummaryDTO e) {
        if (e.getPerformanceType() != null) {
            return switch (e.getPerformanceType()) {
                case THEATER -> "theater";
                case STANDUP -> "standup";
                default -> "theater";
            };
        }
        return e.getType() != null ? e.getType().name().toLowerCase(Locale.ROOT).replace("performing_art", "theater") : "concert";
    }
}
