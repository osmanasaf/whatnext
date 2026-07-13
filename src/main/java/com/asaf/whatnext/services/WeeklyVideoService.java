package com.asaf.whatnext.services;

import com.asaf.whatnext.dto.EventSummaryDTO;
import com.asaf.whatnext.dto.WeeklyPopularEventsDTO;
import com.asaf.whatnext.services.images.RemotionRunner;
import com.asaf.whatnext.utils.EventIdentity;
import com.asaf.whatnext.utils.TrClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class WeeklyVideoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WeeklyVideoService.class);

    @Autowired
    private PopularEventService popularEventService;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private InstagramService instagramService;

    @Autowired
    private RemotionRunner remotionRunner;

    @Value("${whatnext.dedup.title-jaccard:0.5}")
    private double jaccardThreshold;

    @Value("${whatnext.dedup.title-jaccard-strong:0.85}")
    private double jaccardStrongThreshold;

    /** Kategorinin ilk (en yüksek skorlu) etkinliği {@code featured=true} — Reel'de daha uzun süre gösterilir. */
    record SelectedEvent(EventSummaryDTO event, boolean featured) {
    }

    public void generateAndShareWeeklyReel() {
        try {
            WeeklyPopularEventsDTO weekly = popularEventService.getWeeklyPopularEvents();
            List<SelectedEvent> events = collectEventsForVideo(weekly);
            if (events.size() < 3) {
                LOGGER.warn("Yeterli etkinlik yok, video oluşturulmayacak. Etkinlik sayısı: {}", events.size());
                return;
            }

            File videoFile = renderVideo(events, weekly.getWeekRange());
            if (videoFile == null || !videoFile.exists()) {
                LOGGER.error("Video render edilemedi");
                return;
            }

            String videoUrl = uploadVideo(videoFile);
            if (videoUrl != null) {
                shareReelToInstagram(videoUrl, weekly.getWeekRange(), events);
            }

            try {
                Files.deleteIfExists(videoFile.toPath());
            } catch (IOException e) {
                LOGGER.warn("Geçici video dosyası silinemedi: {}", videoFile.getPath());
            }
        } catch (Exception e) {
            LOGGER.error("Haftalık video oluşturma/paylaşım hatası", e);
        }
    }

    /**
     * Kategori başına en iyi 3 (PopularEventService zaten skora göre sıralı veriyor), sonra
     * {@code startDate}+{@code startTime}'a göre gün sıralı — Reel kategoriye değil haftanın
     * akışına göre ilerlesin diye ("Cuma akşamı ne var?").
     */
    List<SelectedEvent> collectEventsForVideo(WeeklyPopularEventsDTO weekly) {
        List<SelectedEvent> result = new ArrayList<>();
        List<EventSummaryDTO> seen = new ArrayList<>();
        addUpTo(result, weekly.getConcerts(), 3, seen);
        addUpTo(result, weekly.getTheaters(), 3, seen);
        addUpTo(result, weekly.getStandups(), 3, seen);
        result.sort(Comparator
                .comparing((SelectedEvent s) -> s.event().getStartDate(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(s -> s.event().getStartTime(), Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    /**
     * Aynı etkinlik (farklı kaynak / çift kayıt) Reel'de iki kez çıkmasın — birebir id eşleşmesinin
     * yanında {@link EventIdentity} ile siteler arası (farklı isimli) aynı etkinlik de eşlenir.
     */
    private void addUpTo(List<SelectedEvent> target, List<EventSummaryDTO> source, int max, List<EventSummaryDTO> seen) {
        if (source == null) {
            return;
        }
        int added = 0;
        for (EventSummaryDTO e : source) {
            if (added >= max) {
                break;
            }
            if (e == null || isDuplicate(seen, e)) {
                continue;
            }
            seen.add(e);
            target.add(new SelectedEvent(e, added == 0));
            added++;
        }
    }

    private boolean isDuplicate(List<EventSummaryDTO> seen, EventSummaryDTO candidate) {
        for (EventSummaryDTO s : seen) {
            if (candidate.getId() != null && candidate.getId().equals(s.getId())) {
                return true;
            }
            if (EventIdentity.isSameEvent(s, candidate, jaccardThreshold, jaccardStrongThreshold)) {
                return true;
            }
        }
        return false;
    }

    private File renderVideo(List<SelectedEvent> selected, String weekRange) {
        try {
            List<Map<String, Object>> eventMaps = new ArrayList<>();
            for (SelectedEvent se : selected) {
                EventSummaryDTO e = se.event();
                String type = "CONCERT";
                if (e.getPerformanceType() != null) {
                    type = e.getPerformanceType().name();
                } else if (e.getType() != null && "PERFORMING_ART".equals(e.getType().name())) {
                    type = "THEATER";
                } else if (e.getType() != null) {
                    type = e.getType().name();
                }
                String dayName = e.getStartDate() != null
                        ? e.getStartDate().getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("tr"))
                        : "";
                Map<String, Object> eventMap = new HashMap<>();
                eventMap.put("id", e.getId() != null ? e.getId() : 0);
                eventMap.put("title", e.getTitle() != null ? e.getTitle() : "");
                eventMap.put("startDate", e.getStartDate() != null ? e.getStartDate().toString() : "");
                eventMap.put("dayName", dayName);
                // Pazartesi=0 ... Pazar=6 — hafta ilerleme çubuğunda tarih parse etmeden doğrudan kullanılır.
                eventMap.put("dayIndex", e.getStartDate() != null ? e.getStartDate().getDayOfWeek().getValue() - 1 : -1);
                eventMap.put("startTime", e.getStartTime() != null ? e.getStartTime().toString() : "");
                eventMap.put("type", type);
                eventMap.put("venueName", e.getVenueName() != null ? e.getVenueName() : "");
                eventMap.put("imageUrl", e.getImageUrl() != null ? e.getImageUrl() : "");
                eventMap.put("featured", se.featured());
                eventMaps.add(eventMap);
            }

            int backgroundVariant = TrClock.today().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) % 5;
            Map<String, Object> props = new HashMap<>();
            props.put("events", eventMaps);
            props.put("weekRange", weekRange);
            props.put("backgroundVariant", backgroundVariant);

            return remotionRunner.run("render", "WeeklyReel", props, "mp4");
        } catch (Exception e) {
            LOGGER.error("Remotion render hatası", e);
            return null;
        }
    }

    private String uploadVideo(File videoFile) {
        try {
            String folder = "whatnext/reels/" + TrClock.today();
            Map<String, Object> result = cloudinaryService.uploadVideo(videoFile, folder);
            return (String) result.get("secure_url");
        } catch (Exception e) {
            LOGGER.error("Video Cloudinary yükleme hatası", e);
            return null;
        }
    }

    private static final String REELS_HASHTAGS = " #istanbul #istanbuletkinlik #etkinlik #konser #tiyatro #müzik #music #concert #festival #istanbulnight #istanbulnights #istanbullife #istanbuldayasam #igersistanbul #sanat #eğlence #eglence #standupistanbul #taksim #kadikoy";

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private void shareReelToInstagram(String videoUrl, String weekRange, List<SelectedEvent> events) {
        try {
            String caption = buildReelCaption(weekRange, events);
            instagramService.shareReel(videoUrl, caption);
        } catch (Exception e) {
            LOGGER.error("Instagram Reel paylaşım hatası", e);
        }
    }

    /** Gün-gün kısa etkinlik listesi (başlık • gün • saat) — Reel'in görsel akışıyla aynı bilgiyi metinde de verir. */
    String buildReelCaption(String weekRange, List<SelectedEvent> events) {
        StringBuilder sb = new StringBuilder();
        sb.append("🎭 Bu hafta İstanbul'da ").append(weekRange).append(" arası kaçırılmayacak etkinlikler!\n\n");
        for (SelectedEvent se : events) {
            EventSummaryDTO e = se.event();
            sb.append("• ").append(e.getTitle());
            if (e.getStartDate() != null) {
                String dayName = e.getStartDate().getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("tr"));
                sb.append(" · ").append(dayName);
            }
            if (e.getStartTime() != null) {
                sb.append(" ").append(e.getStartTime().format(HHMM));
            }
            sb.append('\n');
        }
        sb.append("\n🔖 Kaydet, hafta içinde lazım olacak!\n\n🔗 Detaylar profilde").append(REELS_HASHTAGS);
        return sb.toString();
    }
}
