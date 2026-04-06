package com.asaf.whatnext.services;

import com.asaf.whatnext.dto.EventSummaryDTO;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.enums.PerformanceType;
import com.asaf.whatnext.mapper.EventMapper;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.models.PerformingArt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ImageGenerationService {

    private static final int STORY_EVENT_TARGET = 10;
    private static final int STORY_FETCH_LIMIT = 14;

    @Autowired
    private EventService eventService;

    @Autowired
    private EventMapper eventMapper;

    private static final String OUTPUT_DIR = System.getProperty("java.io.tmpdir");

    @Transactional
    public File createPost(String eventType, String postType, String city, LocalDate date) throws IOException, InterruptedException {
        String monthName = date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();
        String templateResource = "instagram-posts/" + monthName + "/" + postType + "-template.png";
        String coordinatesResource = resolveCoordinatesResource(monthName, postType);

        File templateFile = copyResourceToTempFile(templateResource, ".png");
        File fontFile = copyResourceToTempFile("NotoSans-Regular.ttf", ".ttf");
        File titleFontFile = copyResourceToTempFile("AlfaSlabOne-Regular.ttf", ".ttf");
        File coordinatesFile = copyResourceToTempFile(coordinatesResource, ".properties");

        Properties coordinates = loadPropertiesUtf8(coordinatesFile);

        EventType eventTypeEnum;
        final PerformanceType targetPerformanceType;

        if ("theater".equalsIgnoreCase(eventType)) {
            eventTypeEnum = EventType.PERFORMING_ART;
            targetPerformanceType = PerformanceType.THEATER;
        } else if ("standup".equalsIgnoreCase(eventType)) {
            eventTypeEnum = EventType.PERFORMING_ART;
            targetPerformanceType = PerformanceType.STANDUP;
        } else {
            eventTypeEnum = EventType.valueOf(eventType.toUpperCase());
            targetPerformanceType = null;
        }

        List<Event> events = eventService.findTop6EventsForDateAndCity(date, eventTypeEnum, city);

        if (targetPerformanceType != null) {
            events = events.stream()
                    .filter(event -> event instanceof PerformingArt performingArt &&
                            performingArt.getPerformanceType() == targetPerformanceType)
                    .limit(6)
                    .toList();
        }

        if (events.isEmpty()) {
            System.out.println("⚠️ " + date + " tarihinde " + eventType + " türünde etkinlik bulunamadı. Görsel üretimi atlanıyor.");
            return null;
        }

        for (Event event : events) {
            if (event.getVenue() != null) {
                event.getVenue().getName();
            }
        }

        List<EventSummaryDTO> eventSummaries = events.stream()
                .map(eventMapper::toSummaryDTO)
                .collect(Collectors.toList());

        String outputFilename = "instagram-" + eventType + "-" + postType + "-" + System.currentTimeMillis() + ".png";
        Path outputPath = Paths.get(OUTPUT_DIR, "whatnext", outputFilename);
        Files.createDirectories(outputPath.getParent());

        int[] wh = readImageDimensions(templateFile);
        List<String> command = buildImageMagickCommand(
                templateFile.getAbsolutePath(),
                outputPath.toString(),
                getTurkishEventType(eventType),
                eventSummaries,
                coordinates,
                titleFontFile.getAbsolutePath(),
                fontFile.getAbsolutePath(),
                wh[0],
                wh[1]);
        return executeCommand(command, outputPath.toString());
    }

    public File createCover(LocalDate date) throws IOException, InterruptedException {
        String monthName = date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();
        String templateResource = "instagram-posts/" + monthName + "/cover-template.png";
        String coordinatesResource = "instagram-posts/" + monthName + "/cover-coordinates.properties";

        File templateFile = copyResourceToTempFile(templateResource, ".png");
        File fontFile = copyResourceToTempFile("Audiowide-Regular.ttf", ".ttf");
        File coordinatesFile = copyResourceToTempFile(coordinatesResource, ".properties");

        Properties coordinates = loadPropertiesUtf8(coordinatesFile);

        String outputFilename = "instagram-cover-" + System.currentTimeMillis() + ".png";
        Path outputPath = Paths.get(OUTPUT_DIR, "whatnext", outputFilename);
        Files.createDirectories(outputPath.getParent());

        List<String> command = new ArrayList<>();
        command.add("magick");
        command.add(templateFile.getAbsolutePath());
        command.add("-colorspace");
        command.add("sRGB");
        command.add("-font");
        command.add(fontFile.getAbsolutePath());
        command.add("-gravity");
        command.add("center");
        applyFillAndOptionalStroke(command, coordinates, "date",
                "rgba(40,40,40,0.45)", "2", "white");
        String dateText = date.getDayOfMonth() + " " + date.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("tr"));
        String pointsize = coordinates.getProperty("date.pointsize", "80");
        String geometry = coordinates.getProperty("date.geometry", "+0+0");
        command.add("-pointsize");
        command.add(pointsize);
        command.add("-annotate");
        command.add(geometry);
        command.add(sanitizeAnnotateText(dateText));
        command.add("-stroke");
        command.add("none");
        appendOutputQuality(command);
        command.add(outputPath.toString());
        return executeCommand(command, outputPath.toString());
    }

    @Transactional
    public File createSmartStory(LocalDate date) throws IOException, InterruptedException {
        String monthName = date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();
        String templateResource = "instagram-posts/" + monthName + "/story-template.png";
        String coordinatesResource = "instagram-posts/" + monthName + "/story-coordinates.properties";

        File templateFile = copyResourceToTempFile(templateResource, ".png");
        File fontFile = copyResourceToTempFile("NotoSans-Regular.ttf", ".ttf");
        File titleFontFile = copyResourceToTempFile("AlfaSlabOne-Regular.ttf", ".ttf");
        File coordinatesFile = copyResourceToTempFile(coordinatesResource, ".properties");

        Properties coordinates = loadPropertiesUtf8(coordinatesFile);

        List<Event> allPerformingArts = eventService.findTopNEventsForDateAndCity(date, EventType.PERFORMING_ART, "istanbul",
                STORY_FETCH_LIMIT);
        List<Event> theaters = new ArrayList<>();
        List<Event> standups = new ArrayList<>();
        for (Event e : allPerformingArts) {
            if (e instanceof PerformingArt pa) {
                if (pa.getPerformanceType() == PerformanceType.THEATER) {
                    theaters.add(e);
                } else if (pa.getPerformanceType() == PerformanceType.STANDUP) {
                    standups.add(e);
                }
            }
        }
        List<Event> concerts = new ArrayList<>(eventService.findTopNEventsForDateAndCity(date, EventType.CONCERT, "istanbul",
                STORY_FETCH_LIMIT));
        Collections.shuffle(concerts);
        Collections.shuffle(theaters);
        Collections.shuffle(standups);
        Set<Event> selected = new LinkedHashSet<>();
        addUpTo(selected, concerts, 4);
        addUpTo(selected, theaters, 4);
        addUpTo(selected, standups, 2);
        if (selected.size() < STORY_EVENT_TARGET) {
            List<Event> filler = Stream.of(concerts, theaters, standups)
                    .flatMap(List::stream)
                    .filter(e -> !selected.contains(e))
                    .distinct()
                    .collect(Collectors.toList());
            Collections.shuffle(filler);
            addUpTo(selected, filler, STORY_EVENT_TARGET - selected.size());
        }

        if (selected.isEmpty()) {
            System.out.println("⚠️ " + date + " tarihinde hiç etkinlik bulunamadı. Story görseli üretimi atlanıyor.");
            return null;
        }

        List<EventSummaryDTO> eventSummaries = selected.stream()
                .limit(STORY_EVENT_TARGET)
                .map(eventMapper::toSummaryDTO)
                .toList();

        String outputFilename = "instagram-story-" + System.currentTimeMillis() + ".png";
        Path outputPath = Paths.get(OUTPUT_DIR, "whatnext", outputFilename);
        Files.createDirectories(outputPath.getParent());

        int[] wh = readImageDimensions(templateFile);
        List<String> command = buildImageMagickCommand(
                templateFile.getAbsolutePath(),
                outputPath.toString(),
                "Bugün",
                eventSummaries,
                coordinates,
                titleFontFile.getAbsolutePath(),
                fontFile.getAbsolutePath(),
                wh[0],
                wh[1]);
        return executeCommand(command, outputPath.toString());
    }

    private static String resolveCoordinatesResource(String monthName, String postType) {
        String specific = "instagram-posts/" + monthName + "/" + postType + "-coordinates.properties";
        if (ImageGenerationService.class.getClassLoader().getResource(specific) != null) {
            return specific;
        }
        return "instagram-posts/" + monthName + "/post-coordinates.properties";
    }

    private static Properties loadPropertiesUtf8(File file) throws IOException {
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        if (!content.isEmpty() && content.charAt(0) == '\uFEFF') {
            content = content.substring(1);
        }
        Properties p = new Properties();
        p.load(new StringReader(content));
        return p;
    }

    private void addUpTo(Set<Event> targetSet, List<Event> sourceList, int maxCount) {
        int added = 0;
        for (Event e : sourceList) {
            if (targetSet.add(e)) {
                if (++added >= maxCount) {
                    break;
                }
            }
        }
    }

    private File copyResourceToTempFile(String resourcePath, String suffix) throws IOException {
        InputStream input = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (input == null) {
            throw new IOException("Resource not found: " + resourcePath);
        }
        File tempFile = File.createTempFile("whatnext-resource-", suffix);
        tempFile.deleteOnExit();
        Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        input.close();
        return tempFile;
    }

    private String getTurkishEventType(String eventType) {
        return switch (eventType.toLowerCase(Locale.ROOT)) {
            case "concert" -> "KONSER";
            case "theater" -> "TİYATRO";
            case "standup" -> "STAND-UP";
            default -> eventType.toUpperCase(Locale.ROOT);
        };
    }

    private List<String> buildImageMagickCommand(String templatePath, String outputPath, String eventTypeLabel,
            List<EventSummaryDTO> events, Properties coordinates, String titleFontPath, String bodyFontPath,
            int imageWidth, int imageHeight) {
        if (isLayoutAuto(coordinates)) {
            return buildImageMagickCommandAuto(templatePath, outputPath, eventTypeLabel, events, coordinates,
                    titleFontPath, bodyFontPath, imageWidth, imageHeight);
        }
        return buildImageMagickCommandLegacy(templatePath, outputPath, eventTypeLabel, events, coordinates,
                titleFontPath, bodyFontPath);
    }

    private static boolean isLayoutAuto(Properties p) {
        return "true".equalsIgnoreCase(propTrimOr(p, "layout.auto", "false").trim());
    }

    /** ImageMagick -annotate +x+y; negatif x için \"+-45+100\" üretmeyip String.format kullan. */
    private static String annotateNorthOffset(int x, int y) {
        return String.format("%+d%+d", x, y);
    }

    private static List<String> buildImageMagickCommandLegacy(String templatePath, String outputPath, String eventTypeLabel,
            List<EventSummaryDTO> events, Properties coordinates, String titleFontPath, String bodyFontPath) {
        List<String> command = new ArrayList<>();
        command.add("magick");
        command.add(templatePath);
        command.add("-colorspace");
        command.add("sRGB");

        command.add("-font");
        command.add(titleFontPath);
        command.add("-gravity");
        command.add("north");
        addEventTypeAnnotationLegacy(command, coordinates, eventTypeLabel);

        command.add("-font");
        command.add(bodyFontPath);
        command.add("-gravity");
        command.add("north");
        for (int i = 0; i < events.size(); i++) {
            EventSummaryDTO event = events.get(i);
            addTitleAnnotationLegacy(command, coordinates, "event_" + (i + 1), event.getTitle());
            String venueText = event.getVenueName() != null ? "@" + event.getVenueName() : "";
            addVenueAnnotationLegacy(command, coordinates, "venue_" + (i + 1), venueText);
        }

        appendOutputQuality(command);
        command.add(outputPath);
        return command;
    }

    private static List<String> buildImageMagickCommandAuto(String templatePath, String outputPath, String eventTypeLabel,
            List<EventSummaryDTO> events, Properties coordinates, String titleFontPath, String bodyFontPath,
            int w, int h) {
        double lf = clamp01(readFractionProp(coordinates, "layout.region.left", 0.16));
        double tf = clamp01(readFractionProp(coordinates, "layout.region.top", 0.24));
        double wf = clamp01(readFractionProp(coordinates, "layout.region.width", 0.68));
        double hf = clamp01(readFractionProp(coordinates, "layout.region.height", 0.62));
        wf = Math.min(wf, 1.0 - lf);
        hf = Math.min(hf, 1.0 - tf);

        int L = (int) Math.round(lf * w);
        int T = (int) Math.round(tf * h);
        int rw = (int) Math.round(wf * w);
        int rh = (int) Math.round(hf * h);
        int xCenterOffset = (int) Math.round(L + rw / 2.0 - w / 2.0);

        int n = Math.max(events.size(), 1);
        int ptType0 = clampInt((int) Math.round(rw * 0.048), 28, 58);
        int ptTitle0 = clampInt((int) Math.round(rw * 0.032), 18, 42);
        int ptVenue0 = clampInt((int) Math.round(rw * 0.024), 14, 30);

        int minPadTop = Math.max((int) Math.round(0.048 * rh), 14);
        int bottomPad = Math.max((int) Math.round(0.055 * rh), 18);

        int interRow = n >= 6 ? 15 : (n >= 5 ? 17 : 19);

        double scale = 1.0;
        int ptType = ptType0;
        int ptTitle = ptTitle0;
        int ptVenue = ptVenue0;
        int padTop = minPadTop;
        double block = 40.0;
        int reqBlock = 40;

        for (int iter = 0; iter < 28; iter++) {
            ptType = clampInt((int) Math.round(ptType0 * scale), 18, 58);
            ptTitle = clampInt((int) Math.round(ptTitle0 * scale), 12, 42);
            ptVenue = clampInt((int) Math.round(ptVenue0 * scale), 10, 30);

            reqBlock = computeMaxEventRowHeightPx(events, rw, ptTitle, ptVenue, interRow);

            int minTotal = minPadTop + n * reqBlock + bottomPad;
            if (minTotal <= rh || scale <= 0.5) {
                break;
            }
            scale *= 0.9;
        }

        int[] gapTitleVenuePerRow = computeGapTitleVenuePerRow(events, rw, ptTitle);
        reqBlock = computeMaxEventRowHeightPx(events, rw, ptTitle, ptVenue, interRow);

        padTop = minPadTop;
        int bodySpace = rh - bottomPad - padTop;
        if (reqBlock * n <= bodySpace) {
            block = Math.max(reqBlock, 22.0);
        } else {
            block = Math.max((double) bodySpace / n, 22.0);
        }

        int typeLift = (int) Math.round(ptType * 1.06) + Math.max(14, (int) Math.round(0.02 * h));
        int yHeader = T - typeLift;
        int minYFromTop = Math.max((int) Math.round(0.016 * h), 6);
        yHeader = Math.max(minYFromTop, yHeader);
        int bodyStart = T + padTop;

        List<String> command = new ArrayList<>();
        command.add("magick");
        command.add(templatePath);
        command.add("-colorspace");
        command.add("sRGB");

        command.add("-font");
        command.add(titleFontPath);
        command.add("-gravity");
        command.add("north");
        addEventTypeAnnotationExplicit(command, coordinates, eventTypeLabel,
                String.valueOf(ptType), annotateNorthOffset(xCenterOffset, yHeader));

        command.add("-font");
        command.add(bodyFontPath);
        command.add("-gravity");
        command.add("north");
        for (int i = 0; i < events.size(); i++) {
            EventSummaryDTO event = events.get(i);
            int rowTop = (int) Math.round(bodyStart + i * block);
            int yTitle = rowTop + 4;
            int yVenue = yTitle + gapTitleVenuePerRow[i];
            String titleText = event.getTitle() != null ? event.getTitle() : "";
            addTitleAnnotationExplicit(command, coordinates, titleText,
                    String.valueOf(ptTitle), annotateNorthOffset(xCenterOffset, yTitle));
            String venueText = event.getVenueName() != null ? "@" + event.getVenueName() : "";
            addVenueAnnotationExplicit(command, coordinates, venueText,
                    String.valueOf(ptVenue), annotateNorthOffset(xCenterOffset, yVenue));
        }

        appendOutputQuality(command);
        command.add(outputPath);
        return command;
    }

    private static int estimateTitleDisplayLines(String title, int regionWidthPx, int pointsize) {
        if (title == null || title.isBlank()) {
            return 1;
        }
        double charW = Math.max(1.0, pointsize * 0.58);
        int charsPerLine = Math.max(10, (int) (regionWidthPx / charW));
        int lines = (int) Math.ceil(title.length() / (double) charsPerLine);
        return clampInt(lines, 1, 3);
    }

    private static int gapTitleToVenuePx(int ptTitle, int displayLines) {
        return (int) Math.ceil(ptTitle * 1.12 * displayLines + 5);
    }

    private static int[] computeGapTitleVenuePerRow(List<EventSummaryDTO> events, int regionWidthPx, int ptTitle) {
        int[] g = new int[events.size()];
        for (int i = 0; i < events.size(); i++) {
            int lines = estimateTitleDisplayLines(events.get(i).getTitle(), regionWidthPx, ptTitle);
            g[i] = gapTitleToVenuePx(ptTitle, lines);
        }
        return g;
    }

    private static int computeMaxEventRowHeightPx(List<EventSummaryDTO> events, int regionWidthPx,
            int ptTitle, int ptVenue, int interRow) {
        int[] gaps = computeGapTitleVenuePerRow(events, regionWidthPx, ptTitle);
        int max = 22;
        for (int gap : gaps) {
            int row = gap + (int) Math.ceil(ptVenue * 1.3) + interRow;
            max = Math.max(max, row);
        }
        return max;
    }

    private static int[] readImageDimensions(File imageFile) throws IOException {
        BufferedImage img = ImageIO.read(imageFile);
        if (img == null) {
            throw new IOException("Görsel boyutu okunamadı: " + imageFile);
        }
        return new int[]{img.getWidth(), img.getHeight()};
    }

    private static double readFractionProp(Properties p, String key, double defaultValue) {
        String v = p.getProperty(key);
        if (v == null || v.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(v.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static int clampInt(int v, int min, int max) {
        return Math.min(max, Math.max(min, v));
    }

    private static void addEventTypeAnnotationLegacy(List<String> command, Properties coordinates, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        String key = "event_type";
        String pointsize = coordinates.getProperty(key + ".pointsize");
        String geometry = coordinates.getProperty(key + ".geometry");
        if (pointsize == null || geometry == null) {
            return;
        }
        addEventTypeAnnotationExplicit(command, coordinates, text, pointsize, geometry);
    }

    private static void addEventTypeAnnotationExplicit(List<String> command, Properties coordinates, String text,
            String pointsize, String geometry) {
        if (text == null || text.isBlank()) {
            return;
        }
        applyFillAndOptionalStroke(command, coordinates, "event_type",
                "rgba(120,20,40,0.55)", "2", "#ffffff");
        command.add("-pointsize");
        command.add(pointsize);
        command.add("-annotate");
        command.add(geometry);
        command.add(sanitizeAnnotateText(text));
        command.add("-stroke");
        command.add("none");
    }

    private static void addTitleAnnotationLegacy(List<String> command, Properties coordinates, String key, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        String pointsize = coordinates.getProperty(key + ".pointsize");
        String geometry = coordinates.getProperty(key + ".geometry");
        if (pointsize == null || geometry == null) {
            return;
        }
        addTitleAnnotationExplicit(command, coordinates, text, pointsize, geometry);
    }

    private static void addTitleAnnotationExplicit(List<String> command, Properties coordinates, String text,
            String pointsize, String geometry) {
        if (text == null || text.isBlank()) {
            return;
        }
        applyFillAndOptionalStroke(command, coordinates, "event",
                "none", "0", "#1a1a1a");
        command.add("-pointsize");
        command.add(pointsize);
        command.add("-annotate");
        command.add(geometry);
        command.add(sanitizeAnnotateText(text));
        command.add("-stroke");
        command.add("none");
    }

    private static void addVenueAnnotationLegacy(List<String> command, Properties coordinates, String key, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        String pointsize = coordinates.getProperty(key + ".pointsize");
        String geometry = coordinates.getProperty(key + ".geometry");
        if (pointsize == null || geometry == null) {
            return;
        }
        addVenueAnnotationExplicit(command, coordinates, text, pointsize, geometry);
    }

    private static void addVenueAnnotationExplicit(List<String> command, Properties coordinates, String text,
            String pointsize, String geometry) {
        if (text == null || text.isBlank()) {
            return;
        }
        command.add("-stroke");
        command.add("none");
        command.add("-fill");
        command.add(propTrimOr(coordinates, "venue.fill", "#4b5563"));
        command.add("-pointsize");
        command.add(pointsize);
        command.add("-annotate");
        command.add(geometry);
        command.add(sanitizeAnnotateText(text));
    }

    private static double clamp01(double x) {
        if (x < 0) {
            return 0;
        }
        if (x > 1) {
            return 1;
        }
        return x;
    }

    private static void applyFillAndOptionalStroke(List<String> command, Properties p, String prefix,
            String defaultStroke, String defaultStrokewidth, String defaultFill) {
        String fill = propTrimOr(p, prefix + ".fill", defaultFill);
        String stroke = propTrimOr(p, prefix + ".stroke", defaultStroke);
        String sw = propTrimOr(p, prefix + ".strokewidth", defaultStrokewidth);
        boolean noStroke = "none".equalsIgnoreCase(stroke)
                || "0".equals(sw) || "0.0".equals(sw) || sw.isEmpty();
        if (!noStroke) {
            command.add("-stroke");
            command.add(stroke);
            command.add("-strokewidth");
            command.add(sw);
        } else {
            command.add("-stroke");
            command.add("none");
        }
        command.add("-fill");
        command.add(fill);
    }

    private static String propTrimOr(Properties p, String key, String defaultValue) {
        String v = p.getProperty(key);
        if (v == null) {
            return defaultValue;
        }
        v = v.trim();
        return v.isEmpty() ? defaultValue : v;
    }

    private static void appendOutputQuality(List<String> command) {
        command.add("-strip");
        command.add("-quality");
        command.add("96");
    }

    private static String sanitizeAnnotateText(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.replace('\r', ' ').replace('\n', ' ').trim();
        return s.replace("%", "\\%");
    }

    private String detectImageMagickCommand() {
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            if (commandExistsOnPath("magick")) {
                return "magick";
            }
            if (commandExistsOnPath("convert")) {
                return "convert";
            }
            String pf = System.getenv("ProgramFiles");
            if (pf != null) {
                File magick = new File(pf, "ImageMagick-7.1.1-Q16-HDRI/magick.exe");
                if (magick.isFile()) {
                    return magick.getAbsolutePath();
                }
            }
            return "magick";
        }
        try {
            ProcessBuilder pb = new ProcessBuilder("which", "magick");
            Process p = pb.start();
            if (p.waitFor() == 0) {
                return "magick";
            }
            pb = new ProcessBuilder("which", "convert");
            p = pb.start();
            if (p.waitFor() == 0) {
                return "convert";
            }
        } catch (Exception e) {
            System.out.println("[DEBUG] magick/convert tespiti: " + e.getMessage());
        }
        return "magick";
    }

    private static boolean commandExistsOnPath(String name) {
        try {
            ProcessBuilder pb = new ProcessBuilder("where", name);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private File executeCommand(List<String> command, String outputPath) throws IOException, InterruptedException {
        System.out.println("ℹ️ ImageMagick çalıştırılıyor → " + outputPath);
        String magickCmd = detectImageMagickCommand();
        if (!command.isEmpty() && ("magick".equals(command.get(0)) || "convert".equals(command.get(0)))) {
            command.set(0, magickCmd);
        }
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[ImageMagick] " + line);
            }
        }
        int exitCode = process.waitFor();
        if (exitCode == 0) {
            System.out.println("✅ Görsel oluşturuldu: " + outputPath);
            return new File(outputPath);
        }
        System.err.println("❌ ImageMagick hata. Çıkış kodu: " + exitCode);
        return null;
    }
}
