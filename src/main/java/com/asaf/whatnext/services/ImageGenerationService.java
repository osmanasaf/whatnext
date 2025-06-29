package com.asaf.whatnext.services;

import com.asaf.whatnext.dto.EventSummaryDTO;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.enums.PerformanceType;
import com.asaf.whatnext.mapper.EventMapper;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.models.PerformingArt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.LinkedHashSet;
import java.util.stream.Stream;

@Service
public class ImageGenerationService {

    @Autowired
    private EventService eventService;

    @Autowired
    private EventMapper eventMapper;

    private static final String FONT_PATH = "src/main/resources/NotoSans-Regular.ttf";
    private static final String BASE_RESOURCE_PATH = "src/main/resources/instagram-posts/";
    private static final String OUTPUT_DIR = System.getProperty("java.io.tmpdir");

    public File createPost(String eventType, String postType, String city, LocalDate date) throws IOException, InterruptedException {
        String monthName = date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();
        String templateResource = "instagram-posts/" + monthName + "/" + postType + "-template.png";
        String coordinatesResource = "instagram-posts/" + monthName + "/" + postType + "-coordinates.properties";
        File templateFile = copyResourceToTempFile(templateResource, ".png");
        File fontFile = copyResourceToTempFile("NotoSans-Regular.ttf", ".ttf");
        File alfaFontFile = copyResourceToTempFile("AlfaSlabOne-Regular.ttf", ".ttf");
        File coordinatesFile = copyResourceToTempFile(coordinatesResource, ".properties");
        String fileType = postType.equals("story") ? "story" : "post";
        String outputFilename = "instagram-" + eventType + "-" + fileType + "-" + System.currentTimeMillis() + ".png";
        Path outputPath = Paths.get(OUTPUT_DIR, "whatnext", outputFilename);
        Files.createDirectories(outputPath.getParent());
        Properties coordinates = new Properties();
        try (InputStream input = Files.newInputStream(coordinatesFile.toPath())) {
            coordinates.load(input);
        }
        List<Event> events = eventService.findTop6EventsForDateAndCity(date, EventType.valueOf(eventType.toUpperCase()), city);
        List<EventSummaryDTO> eventSummaries = events.stream()
                .map(eventMapper::toSummaryDTO)
                .collect(Collectors.toList());
        List<String> command = buildImageMagickCommand(templateFile.getAbsolutePath(), outputPath.toString(), eventType, eventSummaries, coordinates, alfaFontFile.getAbsolutePath(), fontFile.getAbsolutePath());
        return executeCommand(command, outputPath.toString());
    }

    public File createCover(LocalDate date) throws IOException, InterruptedException {
        String monthName = date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();
        String templateResource = "instagram-posts/" + monthName + "/cover-template.png";
        String coordinatesResource = "instagram-posts/" + monthName + "/cover-coordinates.properties";
        File templateFile = copyResourceToTempFile(templateResource, ".png");
        File fontFile = copyResourceToTempFile("Audiowide-Regular.ttf", ".ttf");
        File coordinatesFile = copyResourceToTempFile(coordinatesResource, ".properties");
        String outputFilename = "instagram-cover-" + System.currentTimeMillis() + ".png";
        Path outputPath = Paths.get(OUTPUT_DIR, "whatnext", outputFilename);
        Files.createDirectories(outputPath.getParent());
        Properties coordinates = new Properties();
        try (InputStream input = Files.newInputStream(coordinatesFile.toPath())) {
            coordinates.load(input);
        }
        List<String> command = new ArrayList<>();
        command.add("magick");
        command.add(templateFile.getAbsolutePath());
        command.add("-font");
        command.add(fontFile.getAbsolutePath());
        command.add("-fill");
        command.add("white");
        command.add("-gravity");
        command.add("center");
        String dateText = date.getDayOfMonth() + " " + date.getMonth().getDisplayName(TextStyle.FULL, new Locale("tr"));
        String pointsize = coordinates.getProperty("date.pointsize", "80");
        String geometry = coordinates.getProperty("date.geometry", "+0+0");
        command.add("-pointsize");
        command.add(pointsize);
        command.add("-annotate");
        command.add(geometry);
        command.add(dateText);
        command.add(outputPath.toString());
        return executeCommand(command, outputPath.toString());
    }

    public File createSmartStory(LocalDate date) throws IOException, InterruptedException {
        String monthName = date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();
        String templateResource = "instagram-posts/" + monthName + "/story-template.png";
        String coordinatesResource = "instagram-posts/" + monthName + "/story-coordinates.properties";
        File templateFile = copyResourceToTempFile(templateResource, ".png");
        File fontFile = copyResourceToTempFile("NotoSans-Regular.ttf", ".ttf");
        File alfaFontFile = copyResourceToTempFile("AlfaSlabOne-Regular.ttf", ".ttf");
        File coordinatesFile = copyResourceToTempFile(coordinatesResource, ".properties");
        String outputFilename = "instagram-story-" + System.currentTimeMillis() + ".png";
        Path outputPath = Paths.get(OUTPUT_DIR, "whatnext", outputFilename);
        Files.createDirectories(outputPath.getParent());
        Properties coordinates = new Properties();
        try (InputStream input = Files.newInputStream(coordinatesFile.toPath())) {
            coordinates.load(input);
        }
        List<Event> allPerformingArts = eventService.findTop6EventsForDateAndCity(date, EventType.PERFORMING_ART, "istanbul")
                .stream().filter(e -> e.getTitle().length() <= 40).toList();
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
        List<Event> concerts = new ArrayList<>(eventService.findTop6EventsForDateAndCity(date, EventType.CONCERT, "istanbul")
                .stream().filter(e -> e.getTitle().length() <= 40).toList());
        Collections.shuffle(concerts);
        Collections.shuffle(theaters);
        Collections.shuffle(standups);
        Set<Event> selected = new LinkedHashSet<>();
        addUpTo(selected, concerts, 3);
        addUpTo(selected, theaters, 3);
        addUpTo(selected, standups, 2);
        if (selected.size() < 8) {
            List<Event> filler = Stream.of(concerts, theaters, standups)
                    .flatMap(List::stream)
                    .filter(e -> !selected.contains(e))
                    .distinct()
                    .collect(Collectors.toList());
            Collections.shuffle(filler);
            addUpTo(selected, filler, 8 - selected.size());
        }
        List<EventSummaryDTO> eventSummaries = selected.stream()
                .limit(8)
                .map(eventMapper::toSummaryDTO)
                .toList();
        List<String> command = buildImageMagickCommand(templateFile.getAbsolutePath(), outputPath.toString(), "Bugün", eventSummaries, coordinates, alfaFontFile.getAbsolutePath(), fontFile.getAbsolutePath());
        return executeCommand(command, outputPath.toString());
    }

    private void addUpTo(Set<Event> targetSet, List<Event> sourceList, int maxCount) {
        int added = 0;
        for (Event e : sourceList) {
            if (targetSet.add(e)) {
                if (++added >= maxCount) break;
            }
        }
    }

    private File copyResourceToTempFile(String resourcePath, String suffix) throws IOException {
        InputStream input = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (input == null) throw new IOException("Resource not found: " + resourcePath);
        File tempFile = File.createTempFile("resource-", suffix);
        tempFile.deleteOnExit();
        Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return tempFile;
    }

    private Properties loadCoordinates(Path path) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        }
        return properties;
    }

    private String getTurkishEventType(String eventType) {
        return switch (eventType.toLowerCase()) {
            case "concert" -> "KONSER";
            case "theater" -> "TİYATRO";
            case "standup" -> "STAND-UP";
            default -> eventType.toUpperCase();
        };
    }

    private List<String> buildImageMagickCommand(String templatePath, String outputPath, String eventType, List<EventSummaryDTO> events, Properties coordinates, String alfaFontPath, String fontPath) {
        List<String> command = new ArrayList<>();
        command.add("magick");
        command.add(templatePath);
        command.add("-font");
        command.add(alfaFontPath);
        command.add("-fill");
        command.add("white");
        command.add("-gravity");
        command.add("north");
        addAnnotation(command, coordinates, "event_type", getTurkishEventType(eventType));
        command.add("-font");
        command.add(fontPath);
        command.add("-fill");
        command.add("black");
        for (int i = 0; i < events.size(); i++) {
            EventSummaryDTO event = events.get(i);
            command.add("-gravity");
            command.add("north");
            addAnnotation(command, coordinates, "event_" + (i + 1), event.getTitle());
            command.add("-gravity");
            command.add("north");
            String venueText = event.getVenueName() != null ? "@" + event.getVenueName() : "";
            addAnnotation(command, coordinates, "venue_" + (i + 1), venueText);
        }
        command.add(outputPath);
        return command;
    }

    private void addAnnotation(List<String> command, Properties coordinates, String key, String text) {
        if (text == null || text.isBlank()) return;

        String pointsize = coordinates.getProperty(key + ".pointsize");
        String geometry = coordinates.getProperty(key + ".geometry");

        if (pointsize != null && geometry != null) {
            command.add("-pointsize");
            command.add(pointsize);
            command.add("-annotate");
            command.add(geometry);
            command.add(text);
        }
    }

    private File executeCommand(List<String> command, String outputPath) throws IOException, InterruptedException {
        System.out.println("ℹ️ ImageMagick komutu çalıştırılıyor. Çıktı dosyası: " + outputPath);
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[ImageMagick] " + line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode == 0) {
            System.out.println("✅ Geçici görsel başarıyla oluşturuldu: " + outputPath);
            return new File(outputPath);
        } else {
            System.err.println("❌ Görsel oluşturulurken hata oluştu. Çıkış kodu: " + exitCode);
            return null;
        }
    }
} 