package com.asaf.whatnext.services;

import com.asaf.whatnext.dto.EventSummaryDTO;
import com.asaf.whatnext.dto.WeeklyPopularEventsDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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

    @Value("${whatnext.video.project-path:video}")
    private String videoProjectPath;

    @Value("${whatnext.video.npx-command:}")
    private String npxCommand;

    @Value("${whatnext.video.chrome-path:}")
    private String chromePath;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void generateAndShareWeeklyReel() {
        try {
            WeeklyPopularEventsDTO weekly = popularEventService.getWeeklyPopularEvents();
            List<EventSummaryDTO> events = collectEventsForVideo(weekly);
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
                shareReelToInstagram(videoUrl, weekly.getWeekRange());
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

    private List<EventSummaryDTO> collectEventsForVideo(WeeklyPopularEventsDTO weekly) {
        List<EventSummaryDTO> result = new ArrayList<>();
        addUpTo(result, weekly.getConcerts(), 3);
        addUpTo(result, weekly.getTheaters(), 3);
        addUpTo(result, weekly.getStandups(), 3);
        return result;
    }

    private void addUpTo(List<EventSummaryDTO> target, List<EventSummaryDTO> source, int max) {
        if (source == null) return;
        for (int i = 0; i < Math.min(max, source.size()); i++) {
            target.add(source.get(i));
        }
    }

    private File renderVideo(List<EventSummaryDTO> events, String weekRange) {
        try {
            List<Map<String, Object>> eventMaps = new ArrayList<>();
            for (EventSummaryDTO e : events) {
                String type = "CONCERT";
                if (e.getPerformanceType() != null) {
                    type = e.getPerformanceType().name();
                } else if (e.getType() != null && "PERFORMING_ART".equals(e.getType().name())) {
                    type = "THEATER";
                } else if (e.getType() != null) {
                    type = e.getType().name();
                }
                eventMaps.add(Map.of(
                        "id", e.getId() != null ? e.getId() : 0,
                        "title", e.getTitle() != null ? e.getTitle() : "",
                        "startDate", e.getStartDate() != null ? e.getStartDate().toString() : "",
                        "type", type,
                        "venueName", e.getVenueName() != null ? e.getVenueName() : "",
                        "imageUrl", e.getImageUrl() != null ? e.getImageUrl() : ""
                ));
            }

            int backgroundVariant = LocalDate.now().get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR) % 5;
            Map<String, Object> props = new java.util.HashMap<>();
            props.put("events", eventMaps);
            props.put("weekRange", weekRange);
            props.put("backgroundVariant", backgroundVariant);

            Path outputDir = Files.createTempDirectory("whatnext-video");
            File outputFile = outputDir.resolve("weekly-reel.mp4").toFile();
            File propsFile = outputDir.resolve("props.json").toFile();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(propsFile, props);

            String npx = resolveNpxCommand();
            List<String> cmd = new ArrayList<>();
            cmd.add(npx);
            cmd.add("remotion");
            cmd.add("render");
            cmd.add("src/index.ts");
            cmd.add("WeeklyReel");
            cmd.add(outputFile.getAbsolutePath());
            cmd.add("--props");
            cmd.add(propsFile.getAbsolutePath());
            if (chromePath != null && !chromePath.isEmpty()) {
                cmd.add("--browser-executable");
                cmd.add(chromePath);
            }
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new File(videoProjectPath).getAbsoluteFile());
            pb.inheritIO();
            pb.redirectErrorStream(true);

            Process process = pb.start();
            int exitCode = process.waitFor();

            try {
                Files.deleteIfExists(propsFile.toPath());
            } catch (IOException ignored) {}

            if (exitCode == 0 && outputFile.exists()) {
                return outputFile;
            }
            return null;
        } catch (Exception e) {
            LOGGER.error("Remotion render hatası", e);
            return null;
        }
    }

    private String resolveNpxCommand() {
        if (npxCommand != null && !npxCommand.isEmpty()) {
            return npxCommand;
        }
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            String programFiles = System.getenv("ProgramFiles");
            if (programFiles != null) {
                File npxInNode = new File(programFiles, "nodejs\\npx.cmd");
                if (npxInNode.exists()) {
                    return npxInNode.getAbsolutePath();
                }
            }
            String programFilesX86 = System.getenv("ProgramFiles(x86)");
            if (programFilesX86 != null) {
                File npxInNode = new File(programFilesX86, "nodejs\\npx.cmd");
                if (npxInNode.exists()) {
                    return npxInNode.getAbsolutePath();
                }
            }
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                File npxInNpm = new File(appData, "npm\\npx.cmd");
                if (npxInNpm.exists()) {
                    return npxInNpm.getAbsolutePath();
                }
            }
            return "npx.cmd";
        }
        return "npx";
    }

    private String uploadVideo(File videoFile) {
        try {
            String folder = "whatnext/reels/" + LocalDate.now();
            Map<String, Object> result = cloudinaryService.uploadVideo(videoFile, folder);
            return (String) result.get("secure_url");
        } catch (Exception e) {
            LOGGER.error("Video Cloudinary yükleme hatası", e);
            return null;
        }
    }

    private static final String REELS_HASHTAGS = " #istanbul #istanbuletkinlik #etkinlik #konser #tiyatro #müzik #music #concert #festival #istanbulnight #istanbulnights #istanbullife #istanbuldayasam #igersistanbul #sanat #eğlence #eglence #standupistanbul #taksim #kadikoy";

    private void shareReelToInstagram(String videoUrl, String weekRange) {
        try {
            String caption = "🎭 Bu hafta İstanbul'da " + weekRange + " arası kaçırılmayacak etkinlikler!\n\n🔗 Detaylar profilde" + REELS_HASHTAGS;
            instagramService.shareReel(videoUrl, caption);
        } catch (Exception e) {
            LOGGER.error("Instagram Reel paylaşım hatası", e);
        }
    }
}
