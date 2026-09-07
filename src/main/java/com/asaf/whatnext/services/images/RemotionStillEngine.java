package com.asaf.whatnext.services.images;

import com.asaf.whatnext.dto.EventSummaryDTO;
import com.asaf.whatnext.services.ImageGenerationService;
import com.asaf.whatnext.services.templates.GeneratedTemplateProvider;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
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

        return switch (request.postType()) {
            case "cover" -> renderCover(request, monthName, palette, backgroundImageUrl);
            case "story" -> renderStory(request, monthName, palette, backgroundImageUrl);
            default -> renderPost(request, monthName, palette, backgroundImageUrl);
        };
    }

    private File renderPost(ImageRenderRequest request, String monthName, Map<String, String> palette,
            String backgroundImageUrl) throws IOException, InterruptedException {
        List<EventSummaryDTO> events = imageGenerationService.selectPostEvents(
                request.eventType(), request.city(), request.date());
        if (events.isEmpty()) {
            return null;
        }
        Map<String, Object> props = new HashMap<>();
        props.put("eventType", request.eventType());
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
        putIfPresent(props, "backgroundImageUrl", backgroundImageUrl);
        return remotionRunner.run("still", "SmartStoryStill", props, "png");
    }

    private File renderCover(ImageRenderRequest request, String monthName, Map<String, String> palette,
            String backgroundImageUrl) throws IOException, InterruptedException {
        int eventCount = imageGenerationService.countIstanbulEventsForDate(request.date());
        // Kapak = karuselin ilk karesi; günün en iyi afişlerinden mozaik (story seçimiyle aynı skor sırası).
        List<EventSummaryDTO> topEvents = imageGenerationService.selectStoryEvents(request.date());
        Map<String, Object> props = new HashMap<>();
        props.put("monthName", monthName);
        props.put("palette", palette);
        props.put("date", request.date().toString());
        props.put("eventCount", eventCount);
        props.put("events", toEventMaps(topEvents));
        putIfPresent(props, "backgroundImageUrl", backgroundImageUrl);
        return remotionRunner.run("still", "CoverStill", props, "png");
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
