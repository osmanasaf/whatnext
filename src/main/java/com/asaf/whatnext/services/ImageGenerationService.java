package com.asaf.whatnext.services;

import com.asaf.whatnext.dto.EventSummaryDTO;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.enums.PerformanceType;
import com.asaf.whatnext.mapper.EventMapper;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.models.PerformingArt;
import com.asaf.whatnext.services.scoring.EventScoringStrategy;
import com.asaf.whatnext.services.templates.TemplateAssets;
import com.asaf.whatnext.services.templates.TemplateProvider;
import com.asaf.whatnext.utils.EventAudience;
import com.asaf.whatnext.utils.EventQuality;
import com.asaf.whatnext.utils.EventDuplicateKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ImageGenerationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageGenerationService.class);

    /** Story'de en çok 8 satır — 10 kalabalık bulundu (2026-09-07 kullanıcı kararı). */
    private static final int STORY_EVENT_TARGET = 9;
    private static final int POST_EVENT_LIMIT = 8;
    private static final int STORY_FETCH_LIMIT = 14;
    private static final int MAX_TITLE_ANNOTATE_LINES = 3;
    private static final int MAX_VENUE_ANNOTATE_LINES = 2;
    private static final String ELLIPSIS = "…";
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    @Autowired
    private EventService eventService;

    @Autowired
    private EventMapper eventMapper;

    @Autowired
    private TemplateProvider templateProvider;

    @Autowired
    private EventScoringStrategy scoringStrategy;

    private static final String OUTPUT_DIR = System.getProperty("java.io.tmpdir");

    @Transactional
    public File createPost(String eventType, String postType, String city, LocalDate date) throws IOException, InterruptedException {
        String monthName = date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();
        TemplateAssets templateAssets = templateProvider.resolve(monthName, postType);
        File templateFile = templateAssets.templatePng();
        Properties coordinates = templateAssets.coordinates();

        File fontFile = copyResourceToTempFile("NotoSans-Regular.ttf", ".ttf");
        File titleFontFile = copyResourceToTempFile("AlfaSlabOne-Regular.ttf", ".ttf");

        List<EventSummaryDTO> eventSummaries = selectPostEvents(eventType, city, date);
        if (eventSummaries.isEmpty()) {
            LOGGER.warn("{} tarihinde {} türünde etkinlik bulunamadı. Görsel üretimi atlanıyor.", date, eventType);
            return null;
        }

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
                wh[1],
                date,
                false);
        return executeCommand(command, outputPath.toString());
    }

    /**
     * Bir tür (concert/theater/standup) için o günün İstanbul'daki en iyi (en çok {@link #POST_EVENT_LIMIT}) etkinliği —
     * render'dan ayrı, çünkü {@code services/images/} altındaki Remotion/AI motorları da aynı seçimi
     * kullanır (tek doğruluk kaynağı).
     */
    @Transactional(readOnly = true)
    public List<EventSummaryDTO> selectPostEvents(String eventType, String city, LocalDate date) {
        // concert/theater/standup → CONCERT/THEATER/STANDUP; findTopN skora göre sıralı döner.
        EventType eventTypeEnum = EventType.valueOf(eventType.toUpperCase(Locale.ROOT));
        List<Event> events = eventService.findTopNEventsForDateAndCity(date, eventTypeEnum, city, 18).stream()
                .filter(event -> !EventAudience.isChildrensEvent(event))
                .filter(event -> !EventQuality.isAmateurNight(event))
                .limit(POST_EVENT_LIMIT)
                .toList();

        for (Event event : events) {
            if (event.getVenue() != null) {
                event.getVenue().getName();
            }
        }

        return events.stream().map(eventMapper::toSummaryDTO).collect(Collectors.toList());
    }

    public File createCover(LocalDate date) throws IOException, InterruptedException {
        String monthName = date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();
        TemplateAssets templateAssets = templateProvider.resolve(monthName, "cover");
        File templateFile = templateAssets.templatePng();
        Properties coordinates = templateAssets.coordinates();

        File fontFile = copyResourceToTempFile("Audiowide-Regular.ttf", ".ttf");

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

        int dailyEventCount = countIstanbulEventsForDate(date);
        if (dailyEventCount > 0) {
            int datePointsizeNum = parseIntOr(pointsize, 80);
            int countPointsizeNum = Math.max(24, datePointsizeNum / 2);
            // Sabit piksel yerine date.pointsize'a orantılı offset — march gibi 2x çözünürlüklü
            // aylarda da (pointsize=144) satır boşluğu doğru ölçeklenir.
            String countGeometry = coordinates.getProperty("date.count.geometry",
                    annotateNorthOffset(0, (int) Math.round(datePointsizeNum * 1.6)));
            String countPointsize = coordinates.getProperty("date.count.pointsize", String.valueOf(countPointsizeNum));
            command.add("-pointsize");
            command.add(countPointsize);
            command.add("-annotate");
            command.add(countGeometry);
            command.add(sanitizeAnnotateText("Bugün " + dailyEventCount + " etkinlik"));
        }

        command.add("-stroke");
        command.add("none");
        appendOutputQuality(command);
        command.add(outputPath.toString());
        return executeCommand(command, outputPath.toString());
    }

    /**
     * İstanbul'daki, çocuk etkinliği olmayan o günkü toplam etkinlik sayısı (kapak alt satırı için) —
     * {@code services/images/} motorları da kullanır.
     */
    @Transactional(readOnly = true)
    public int countIstanbulEventsForDate(LocalDate date) {
        return (int) eventService.findByDate(date.toString()).stream()
                .filter(e -> "istanbul".equalsIgnoreCase(e.getCity()))
                .filter(e -> !EventAudience.isChildrensEvent(e))
                .filter(e -> !EventQuality.isAmateurNight(e))
                .count();
    }

    @Transactional
    public File createSmartStory(LocalDate date) throws IOException, InterruptedException {
        String monthName = date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();
        TemplateAssets templateAssets = templateProvider.resolve(monthName, "story");
        File templateFile = templateAssets.templatePng();
        Properties coordinates = templateAssets.coordinates();

        File fontFile = copyResourceToTempFile("NotoSans-Regular.ttf", ".ttf");
        File titleFontFile = copyResourceToTempFile("AlfaSlabOne-Regular.ttf", ".ttf");

        List<EventSummaryDTO> eventSummaries = selectStoryEvents(date);
        if (eventSummaries.isEmpty()) {
            LOGGER.warn("{} tarihinde hiç etkinlik bulunamadı. Story görseli üretimi atlanıyor.", date);
            return null;
        }

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
                wh[1],
                date,
                true);
        return executeCommand(command, outputPath.toString());
    }

    /**
     * Story için karışık türde (4 konser + 4 tiyatro + 2 stand-up, skor sıralı, dolgu ile 10'a
     * tamamlanır) etkinlik seçimi — render'dan ayrı, {@code services/images/} motorları da kullanır.
     */
    @Transactional(readOnly = true)
    public List<EventSummaryDTO> selectStoryEvents(LocalDate date) {
        List<Event> concerts = topForStory(date, EventType.CONCERT);
        List<Event> theaters = topForStory(date, EventType.THEATER);
        List<Event> standups = topForStory(date, EventType.STANDUP);
        Set<Event> selected = new LinkedHashSet<>();
        addUpTo(selected, concerts, 3);
        addUpTo(selected, theaters, 3);
        addUpTo(selected, standups, 2);
        if (selected.size() < STORY_EVENT_TARGET) {
            List<Event> filler = Stream.of(concerts, theaters, standups)
                    .flatMap(List::stream)
                    .filter(e -> !selected.contains(e))
                    .distinct()
                    .collect(Collectors.toList());
            sortByScoreDescending(filler);
            addUpTo(selected, filler, STORY_EVENT_TARGET - selected.size());
        }

        EventDuplicateKeys.dedupeSetByTitleAndDate(selected);

        return selected.stream().limit(STORY_EVENT_TARGET).map(eventMapper::toSummaryDTO).toList();
    }

    /** Türün skora göre ilk {@code STORY_FETCH_LIMIT} etkinliği, çocuk etkinlikleri hariç. */
    private List<Event> topForStory(LocalDate date, EventType type) {
        List<Event> events = new ArrayList<>(eventService.findTopNEventsForDateAndCity(date, type, "istanbul", STORY_FETCH_LIMIT));
        events.removeIf(e -> EventAudience.isChildrensEvent(e) || EventQuality.isAmateurNight(e));
        return events;
    }

    /** Rastgele değil, {@link EventScoringStrategy} skoruna göre en yüksekten düşüğe (PopularEventService ile tutarlı). */
    private void sortByScoreDescending(List<Event> events) {
        events.sort(Comparator.comparingInt(scoringStrategy::calculateScore).reversed());
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
            int imageWidth, int imageHeight, LocalDate date, boolean isStory) {
        if (isLayoutAuto(coordinates)) {
            return buildImageMagickCommandAuto(templatePath, outputPath, eventTypeLabel, events, coordinates,
                    titleFontPath, bodyFontPath, imageWidth, imageHeight, date, isStory);
        }
        return buildImageMagickCommandLegacy(templatePath, outputPath, eventTypeLabel, events, coordinates,
                titleFontPath, bodyFontPath, imageWidth);
    }

    private static boolean isLayoutAuto(Properties p) {
        return "true".equalsIgnoreCase(propTrimOr(p, "layout.auto", "false").trim());
    }

    /** ImageMagick -annotate +x+y; negatif x için \"+-45+100\" üretmeyip String.format kullan. */
    private static String annotateNorthOffset(int x, int y) {
        return String.format("%+d%+d", x, y);
    }

    private static List<String> buildImageMagickCommandLegacy(String templatePath, String outputPath, String eventTypeLabel,
            List<EventSummaryDTO> events, Properties coordinates, String titleFontPath, String bodyFontPath,
            int fullImageWidth) {
        int legWrapW = Math.max(240, (int) Math.round(fullImageWidth * 0.86));
        Font bodyFont = loadFont(bodyFontPath);
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
            addTitleAnnotationLegacy(command, coordinates, "event_" + (i + 1), event.getTitle(), legWrapW, bodyFont);
            String venueText = buildVenueLineWithTime(event);
            addVenueAnnotationLegacy(command, coordinates, "venue_" + (i + 1), venueText, legWrapW, bodyFont);
        }

        appendOutputQuality(command);
        command.add(outputPath);
        return command;
    }

    static List<String> buildImageMagickCommandAuto(String templatePath, String outputPath, String eventTypeLabel,
            List<EventSummaryDTO> events, Properties coordinates, String titleFontPath, String bodyFontPath,
            int w, int h, LocalDate date, boolean isStory) {
        // Başlık/mekân satırları (ve gün+tarih alt satırı) bodyFontPath ile çiziliyor — sarma/gerçek
        // satır yüksekliği hesapları da aynı fontla ölçülmeli (AlfaSlabOne gibi geniş fontlarda
        // pointsize×sabit tahmini yanıltıcı).
        Font bodyFont = loadFont(bodyFontPath);
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

            reqBlock = computeMaxEventRowHeightPx(events, rw, bodyFont, ptTitle, ptVenue, interRow);

            int minTotal = minPadTop + n * reqBlock + bottomPad;
            if (minTotal <= rh || scale <= 0.5) {
                break;
            }
            scale *= 0.9;
        }

        int[] gapTitleVenuePerRow = computeGapTitleVenuePerRow(events, rw, bodyFont, ptTitle);
        reqBlock = computeMaxEventRowHeightPx(events, rw, bodyFont, ptTitle, ptVenue, interRow);

        padTop = minPadTop;
        int bodySpace = rh - bottomPad - padTop;
        if (reqBlock * n <= bodySpace) {
            block = Math.max(reqBlock, 22.0);
        } else {
            block = Math.max((double) bodySpace / n, 22.0);
        }

        // Gün+tarih alt satırı ("Cuma • 17 Temmuz") tür rozetinin hemen altında, region'ın (T) ÜSTÜNDEKİ
        // header bölgesinde çizilir — mevcut per-event satır bütçesini (block/rh) büyütmez.
        String dayDateLabel = date != null
                ? date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("tr"))
                        + " • " + date.getDayOfMonth() + " " + date.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("tr"))
                : null;
        int ptSubheader = clampInt((int) Math.round(ptType * 0.42), 14, 26);
        int subheaderLift = dayDateLabel != null ? (int) Math.round(ptSubheader * 1.3) + Math.max(6, (int) Math.round(0.01 * h)) : 0;

        int typeLift = (int) Math.round(ptType * 1.06) + Math.max(14, (int) Math.round(0.02 * h)) + subheaderLift;
        int yHeader = T - typeLift;
        int minYFromTop = Math.max((int) Math.round(0.016 * h), 6);
        yHeader = Math.max(minYFromTop, yHeader);
        int ySubheader = yHeader + (int) Math.round(ptType * 1.06) + Math.max(10, (int) Math.round(0.012 * h));
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

        if (dayDateLabel != null) {
            command.add("-font");
            command.add(bodyFontPath);
            command.add("-gravity");
            command.add("north");
            addDayDateAnnotationExplicit(command, coordinates, dayDateLabel,
                    String.valueOf(ptSubheader), annotateNorthOffset(xCenterOffset, ySubheader));
        }

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
            String titleWrapped = wrapForAnnotate(titleText, rw, bodyFont, ptTitle, MAX_TITLE_ANNOTATE_LINES);
            addTitleAnnotationExplicit(command, coordinates, titleWrapped,
                    String.valueOf(ptTitle), annotateNorthOffset(xCenterOffset, yTitle));
            String venueText = buildVenueLineWithTime(event);
            String venueWrapped = venueText.isBlank() ? "" : wrapForAnnotate(venueText, rw, bodyFont, ptVenue, MAX_VENUE_ANNOTATE_LINES);
            addVenueAnnotationExplicit(command, coordinates, venueWrapped,
                    String.valueOf(ptVenue), annotateNorthOffset(xCenterOffset, yVenue));
        }

        if (isStory) {
            // Instagram story UI'ının yanıt çubuğu alt ~%12'yi kapladığı için üstüne, güvenli bölgeye.
            int ptCta = clampInt((int) Math.round(w * 0.026), 16, 30);
            int ctaBottomInset = Math.max((int) Math.round(0.12 * h), 40);
            command.add("-font");
            command.add(bodyFontPath);
            command.add("-gravity");
            command.add("south");
            // NotoSans (annotate fontu) emoji glyph'i içermiyor — resme gömülen metinde emoji "tofu"
            // kutusu olarak çıkar; emoji'ler yalnızca Instagram'ın kendi UI'sinde render olan
            // caption'larda (bkz. InstagramSchedulerService.buildDailyCaption) güvenli.
            addCtaAnnotationExplicit(command, coordinates, "KAYDET • whatnext.tr",
                    String.valueOf(ptCta), annotateNorthOffset(0, ctaBottomInset));
        }

        appendOutputQuality(command);
        command.add(outputPath);
        return command;
    }

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    /** "21:00 • @Mekân" — saat yoksa yeni satır eklemeden mevcut mekân satırına biner (dikey alan bütçesi büyümez). */
    static String buildVenueLineWithTime(EventSummaryDTO event) {
        String venue = event.getVenueName() != null && !event.getVenueName().isBlank()
                ? "@" + event.getVenueName() : "";
        if (event.getStartTime() == null) {
            return venue;
        }
        String time = event.getStartTime().format(HHMM);
        return venue.isBlank() ? time : time + " • " + venue;
    }

    private static int estimateTitleDisplayLines(String title, int regionWidthPx, Font font, int pointsize) {
        if (title == null || title.isBlank()) {
            return 1;
        }
        String w = wrapForAnnotate(title, regionWidthPx, font, pointsize, MAX_TITLE_ANNOTATE_LINES);
        return countVisualLines(w, 1);
    }

    private static int gapTitleToVenuePx(int ptTitle, int displayLines) {
        return (int) Math.ceil(ptTitle * 1.12 * displayLines + 5);
    }

    private static int[] computeGapTitleVenuePerRow(List<EventSummaryDTO> events, int regionWidthPx, Font font, int ptTitle) {
        int[] g = new int[events.size()];
        for (int i = 0; i < events.size(); i++) {
            int lines = estimateTitleDisplayLines(events.get(i).getTitle(), regionWidthPx, font, ptTitle);
            g[i] = gapTitleToVenuePx(ptTitle, lines);
        }
        return g;
    }

    private static int computeMaxEventRowHeightPx(List<EventSummaryDTO> events, int regionWidthPx,
            Font font, int ptTitle, int ptVenue, int interRow) {
        int[] gaps = computeGapTitleVenuePerRow(events, regionWidthPx, font, ptTitle);
        int max = 22;
        for (int i = 0; i < events.size(); i++) {
            int gap = i < gaps.length ? gaps[i] : gapTitleToVenuePx(ptTitle, 1);
            String v = buildVenueLineWithTime(events.get(i));
            int vLines = v.isBlank() ? 0
                    : countVisualLines(wrapForAnnotate(v, regionWidthPx, font, ptVenue, MAX_VENUE_ANNOTATE_LINES), 0);
            int venueH = vLines <= 0 ? 0 : (int) Math.ceil(ptVenue * 1.15 * vLines);
            int row = gap + venueH + interRow;
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

    private static int parseIntOr(String s, int defaultValue) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return defaultValue;
        }
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

    private static void addTitleAnnotationLegacy(List<String> command, Properties coordinates, String key, String text,
            int regionWidthPx, Font font) {
        if (text == null || text.isBlank()) {
            return;
        }
        String pointsize = coordinates.getProperty(key + ".pointsize");
        String geometry = coordinates.getProperty(key + ".geometry");
        if (pointsize == null || geometry == null) {
            return;
        }
        int pt = parseIntOr(pointsize, 32);
        String wrapped = wrapForAnnotate(text, regionWidthPx, font, pt, MAX_TITLE_ANNOTATE_LINES);
        addTitleAnnotationExplicit(command, coordinates, wrapped, pointsize, geometry);
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
        command.add(sanitizeAnnotateTextMultiline(text));
        command.add("-stroke");
        command.add("none");
    }

    private static void addVenueAnnotationLegacy(List<String> command, Properties coordinates, String key, String text,
            int regionWidthPx, Font font) {
        if (text == null || text.isBlank()) {
            return;
        }
        String pointsize = coordinates.getProperty(key + ".pointsize");
        String geometry = coordinates.getProperty(key + ".geometry");
        if (pointsize == null || geometry == null) {
            return;
        }
        int pt = parseIntOr(pointsize, 24);
        String wrapped = wrapForAnnotate(text, regionWidthPx, font, pt, MAX_VENUE_ANNOTATE_LINES);
        addVenueAnnotationExplicit(command, coordinates, wrapped, pointsize, geometry);
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
        command.add(sanitizeAnnotateTextMultiline(text));
    }

    /**
     * Gün+tarih alt satırı — {@code venue.fill} gibi kremrengi kutu-içi renkler değil, tür rozetiyle
     * aynı koyu header zemini üzerine çizilir; okunabilirlik için varsayılan açık renk + hafif stroke.
     */
    private static void addDayDateAnnotationExplicit(List<String> command, Properties coordinates, String text,
            String pointsize, String geometry) {
        if (text == null || text.isBlank()) {
            return;
        }
        applyFillAndOptionalStroke(command, coordinates, "daydate",
                "rgba(0,0,0,0.35)", "1.5", "#ffffff");
        command.add("-pointsize");
        command.add(pointsize);
        command.add("-annotate");
        command.add(geometry);
        command.add(sanitizeAnnotateText(text));
        command.add("-stroke");
        command.add("none");
    }

    /**
     * Story alt güvenli bölgedeki "Kaydet" çağrısı — şablona gömülmez. 12 ayın alt kenarındaki
     * illüstrasyon yoğunluğu farklı (bazılarında boşluk, bazılarında figürler) olduğu için sabit bir
     * renk/stroke yerine {@code -undercolor} ile yarı saydam bir "pill" arka plan çizilir — hangi
     * şablonun altında olursa olsun okunabilir, ay ay ayrı ayarlama gerekmez.
     */
    private static void addCtaAnnotationExplicit(List<String> command, Properties coordinates, String text,
            String pointsize, String geometry) {
        if (text == null || text.isBlank()) {
            return;
        }
        command.add("-stroke");
        command.add("none");
        command.add("-undercolor");
        command.add(propTrimOr(coordinates, "cta.undercolor", "rgba(20,20,20,0.55)"));
        command.add("-fill");
        command.add(propTrimOr(coordinates, "cta.fill", "#ffffff"));
        command.add("-pointsize");
        command.add(pointsize);
        command.add("-annotate");
        command.add(geometry);
        command.add(sanitizeAnnotateText(text));
        command.add("-undercolor");
        command.add("none");
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

    /** ImageMagick -annotate: çok satır; her satırda % kaçışı. */
    private static String sanitizeAnnotateTextMultiline(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        return Stream.of(raw.split("\n", -1))
                .map(line -> line.replace('\r', ' ').trim().replace("%", "\\%"))
                .collect(Collectors.joining("\n"));
    }

    private static int countVisualLines(String wrapped, int emptyDefault) {
        if (wrapped == null || wrapped.isEmpty()) {
            return emptyDefault;
        }
        int n = (int) Stream.of(wrapped.split("\n", -1)).filter(s -> !s.isEmpty()).count();
        return Math.max(1, n);
    }

    /**
     * Metni verilen piksel genişliğine kelime sınırlarında sarar; sığmayan kısım "…" ile kısaltılır.
     * Genişlik gerçek {@link FontMetrics} ile ölçülür (AlfaSlabOne gibi geniş fontlarda
     * {@code pointsize × sabit} tahmininden çok daha isabetli — taşma/erken kesme riskini azaltır).
     */
    static String wrapForAnnotate(String text, int regionWidthPx, Font font, int pointsize, int maxLines) {
        if (text == null) {
            return "";
        }
        String t = WHITESPACE.matcher(text.trim()).replaceAll(" ");
        if (t.isEmpty()) {
            return "";
        }
        FontMetrics fm = fontMetricsFor(font, pointsize);
        String[] words = t.split(" ");
        List<String> lines = new ArrayList<>();
        int i = 0;
        while (i < words.length && lines.size() < maxLines) {
            StringBuilder line = new StringBuilder();
            while (i < words.length) {
                String w = words[i];
                if (w.isEmpty()) {
                    i++;
                    continue;
                }
                String candidate = line.length() > 0 ? line + " " + w : w;
                if (fm.stringWidth(candidate) <= regionWidthPx) {
                    line = new StringBuilder(candidate);
                    i++;
                } else {
                    if (line.length() == 0) {
                        line.append(truncateToWidth(fm, w, regionWidthPx));
                        i++;
                    }
                    break;
                }
            }
            if (line.length() > 0) {
                lines.add(line.toString());
            } else {
                break;
            }
        }
        if (i < words.length && !lines.isEmpty()) {
            String last = lines.get(lines.size() - 1);
            if (!last.endsWith(ELLIPSIS)) {
                String withEllipsis = last + ELLIPSIS;
                if (fm.stringWidth(withEllipsis) <= regionWidthPx) {
                    lines.set(lines.size() - 1, withEllipsis);
                } else {
                    lines.set(lines.size() - 1, truncateToWidth(fm, last, regionWidthPx));
                }
            }
        }
        return String.join("\n", lines);
    }

    /** Tek kelime/satır bölgeye sığmıyorsa "…" ile birlikte sığan en uzun öneke ikili aramayla keser. */
    private static String truncateToWidth(FontMetrics fm, String text, int maxWidthPx) {
        if (fm.stringWidth(text) <= maxWidthPx) {
            return text;
        }
        int lo = 0;
        int hi = text.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            String candidate = text.substring(0, mid) + ELLIPSIS;
            if (fm.stringWidth(candidate) <= maxWidthPx) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return lo > 0 ? text.substring(0, lo) + ELLIPSIS : ELLIPSIS;
    }

    private static FontMetrics fontMetricsFor(Font font, int pointsize) {
        Font sized = font.deriveFont((float) pointsize);
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            return g.getFontMetrics(sized);
        } finally {
            g.dispose();
        }
    }

    /** Font dosyası okunamazsa (beklenmez, classpath fontları) sistem fontuna güvenli düşüş. */
    private static Font loadFont(String fontPath) {
        try {
            return Font.createFont(Font.TRUETYPE_FONT, new File(fontPath));
        } catch (Exception e) {
            LOGGER.warn("Font yüklenemedi ({}), sistem fontuna düşülüyor: {}", fontPath, e.getMessage());
            return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        }
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
            LOGGER.debug("magick/convert tespiti: {}", e.getMessage());
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

    /**
     * ImageMagick başarısız olursa (site/font/komut hatası) {@code null} yerine exception fırlatır —
     * çağıranlar (createPost/createCover/createSmartStory) zaten {@code throws IOException} bildiriyor.
     * Bu, "o gün/tür için etkinlik yok" (bilinçli, erken {@code null} dönüşü) ile "render gerçekten
     * patladı" durumlarının {@link InstagramService#createImage} tarafında birbirine karışmasını önler.
     */
    private File executeCommand(List<String> command, String outputPath) throws IOException, InterruptedException {
        LOGGER.info("ImageMagick çalıştırılıyor → {}", outputPath);
        String magickCmd = detectImageMagickCommand();
        if (!command.isEmpty() && ("magick".equals(command.get(0)) || "convert".equals(command.get(0)))) {
            command.set(0, magickCmd);
        }
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LOGGER.debug("[ImageMagick] {}", line);
                output.append(line).append('\n');
            }
        }
        int exitCode = process.waitFor();
        if (exitCode == 0) {
            LOGGER.info("Görsel oluşturuldu: {}", outputPath);
            return new File(outputPath);
        }
        LOGGER.error("ImageMagick hata. Çıkış kodu: {}, çıktı:\n{}", exitCode, output);
        throw new IOException("ImageMagick başarısız (çıkış kodu " + exitCode + "): " + outputPath);
    }
}
