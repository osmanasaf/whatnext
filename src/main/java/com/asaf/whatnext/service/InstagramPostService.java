package com.asaf.whatnext.service;

import com.asaf.whatnext.dto.EventSummaryDTO;
import com.asaf.whatnext.enums.EventType;
import com.asaf.whatnext.mapper.EventMapper;
import com.asaf.whatnext.models.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Collectors;

@Service
public class InstagramPostService {

    @Autowired
    private EventService eventService;

    @Autowired
    private EventMapper eventMapper;

    private static final String FONT_PATH = "src/main/resources/NotoSans-Regular.ttf";
    private static final String BASE_RESOURCE_PATH = "src/main/resources/instagram-posts/";
    private static final String OUTPUT_DIR = "src/main/resources/instagram-posts/";

    public String createPost(String eventType, String postType, String city, LocalDate date) throws IOException, InterruptedException {
        String monthName = date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();
        Path templatePath = Paths.get(BASE_RESOURCE_PATH, monthName, postType + "-template.png");
        Path coordinatesPath = Paths.get(BASE_RESOURCE_PATH, monthName, postType + "-coordinates.properties");
        String fileType = postType.equals("story") ? "story" : "post";
        Path outputPath = Paths.get(OUTPUT_DIR, monthName, String.valueOf(date.getDayOfMonth()), "instagram-" + eventType + "-" + fileType + ".png");

        Files.createDirectories(outputPath.getParent());

        Properties coordinates = loadCoordinates(coordinatesPath);
        List<Event> events = eventService.findTop6EventsForDateAndCity(date, EventType.valueOf(eventType.toUpperCase()), city);
        List<EventSummaryDTO> eventSummaries = events.stream()
                .map(eventMapper::toSummaryDTO)
                .collect(Collectors.toList());

        List<String> command = buildImageMagickCommand(templatePath.toString(), outputPath.toString(), eventType, eventSummaries, coordinates);

        return executeCommand(command, outputPath.toString());
    }

    public String createCover(LocalDate date) throws IOException, InterruptedException {
        String monthName = date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();
        Path templatePath = Paths.get(BASE_RESOURCE_PATH, monthName, "cover-template.png");
        Path coordinatesPath = Paths.get(BASE_RESOURCE_PATH, monthName, "cover-coordinates.properties");
        Path outputPath = Paths.get(OUTPUT_DIR, monthName, String.valueOf(date.getDayOfMonth()), "instagram-cover.png");

        Files.createDirectories(outputPath.getParent());
        Properties coordinates = loadCoordinates(coordinatesPath);

        List<String> command = new ArrayList<>();
        command.add("magick");
        command.add("convert");
        command.add(templatePath.toString());
        command.add("-font");
        command.add("src/main/resources/Audiowide-Regular.ttf");
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

    public String createSmartStory(LocalDate date) throws IOException, InterruptedException {
        String monthName = date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();
        Path templatePath = Paths.get(BASE_RESOURCE_PATH, monthName, "story-template.png");
        Path coordinatesPath = Paths.get(BASE_RESOURCE_PATH, monthName, "story-coordinates.properties");
        Path outputPath = Paths.get(OUTPUT_DIR, monthName, String.valueOf(date.getDayOfMonth()), "instagram-story.png");
        Files.createDirectories(outputPath.getParent());
        Properties coordinates = loadCoordinates(coordinatesPath);

        List<Event> concerts = eventService.findTop6EventsForDateAndCity(date, EventType.CONCERT, "istanbul")
            .stream().filter(e -> e.getTitle().length() <= 40).collect(Collectors.toList());
        List<Event> performingArts = eventService.findTop6EventsForDateAndCity(date, EventType.PERFORMING_ART, "istanbul")
            .stream().filter(e -> e.getTitle().length() <= 40).collect(Collectors.toList());
        List<Event> theaters = performingArts.stream()
            .filter(e -> e instanceof com.asaf.whatnext.models.PerformingArt pa && pa.getPerformanceType() == com.asaf.whatnext.enums.PerformanceType.THEATER)
            .collect(Collectors.toList());
        List<Event> standups = performingArts.stream()
            .filter(e -> e instanceof com.asaf.whatnext.models.PerformingArt pa && pa.getPerformanceType() == com.asaf.whatnext.enums.PerformanceType.STANDUP)
            .collect(Collectors.toList());

        Collections.shuffle(concerts);
        Collections.shuffle(theaters);
        Collections.shuffle(standups);

        List<Event> selected = new ArrayList<>();
        selected.addAll(concerts.stream().limit(2).toList());
        selected.addAll(theaters.stream().limit(2).toList());
        selected.addAll(standups.stream().limit(2).toList());

        while (selected.size() < 6) {
            List<Event> pool = new ArrayList<>();
            if (selected.stream().filter(e -> e.getType() == EventType.CONCERT).count() < 2) {
                pool.addAll(concerts);
            }
            if (selected.stream().filter(e -> e.getType() == EventType.THEATER).count() < 2) {
                pool.addAll(theaters);
            }
            if (selected.stream().filter(e -> e.getType() == EventType.STANDUP).count() < 2) {
                pool.addAll(standups);
            }
            if (pool.isEmpty()) {
                pool.addAll(concerts);
                pool.addAll(theaters);
                pool.addAll(standups);
            }
            pool.removeAll(selected);
            if (pool.isEmpty()) break;
            Collections.shuffle(pool);
            selected.add(pool.get(0));
        }
        selected = selected.stream().limit(6).collect(Collectors.toList());
        List<EventSummaryDTO> eventSummaries = selected.stream().map(eventMapper::toSummaryDTO).collect(Collectors.toList());
        List<String> command = buildImageMagickCommand(templatePath.toString(), outputPath.toString(), "Günün Etkinlikleri", eventSummaries, coordinates);
        return executeCommand(command, outputPath.toString());
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

    private List<String> buildImageMagickCommand(String templatePath, String outputPath, String eventType, List<EventSummaryDTO> events, Properties coordinates) {
        List<String> command = new ArrayList<>();
        command.add("magick");
        command.add("convert");
        command.add(templatePath);
        command.add("-font");
        command.add("src/main/resources/AlfaSlabOne-Regular.ttf");
        command.add("-fill");
        command.add("white");
        command.add("-gravity");
        command.add("north");
        addAnnotation(command, coordinates, "event_type", getTurkishEventType(eventType));
        command.add("-font");
        command.add(FONT_PATH);
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

    private String executeCommand(List<String> command, String outputPath) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode == 0) {
            System.out.println("✅ Görsel başarıyla oluşturuldu: " + outputPath);
            return outputPath;
        } else {
            System.err.println("❌ Görsel oluşturulurken hata oluştu. Çıkış kodu: " + exitCode);
            return null;
        }
    }
} 