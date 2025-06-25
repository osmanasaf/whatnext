package com.asaf.whatnext.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ImageGenerateService {
    @Value("${openai.api.key}")
    private String apiKey;

    private static final String ENDPOINT = "https://api.openai.com/v1/images/generations";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateAndSaveImage(String prompt, String savePath, String size) {
        try {
            String safePrompt = safePrompt(prompt);
            String json = String.format("""
                {\n  "model": "dall-e-3",\n  "prompt": "%s",\n  "n": 1,\n  "size": "%s",\n  "quality": "standard",\n  "response_format": "b64_json"\n}\n                """, safePrompt.replace("\"", "\\\""), size);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("API Error: " + response.statusCode());
                System.err.println(response.body());
                // Fallback prompt ile tekrar dene
                String fallback = fallbackPrompt();
                if (!fallback.equals(safePrompt)) {
                    return generateAndSaveImage(fallback, savePath, size);
                }
                return null;
            }

            String body = response.body();
            String base64Image = extractBase64FromResponse(body);
            if (base64Image == null) return null;

            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Image);
            Files.createDirectories(Path.of(savePath).getParent());
            try (OutputStream os = new FileOutputStream(savePath)) {
                os.write(imageBytes);
            }

            System.out.println("✅ Görsel kaydedildi: " + savePath);
            return savePath;

        } catch (Exception e) {
            System.err.println("❌ Hata oluştu: " + e.getMessage());
            return null;
        }
    }

    private String extractBase64FromResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("data") && root.get("data").isArray() && root.get("data").size() > 0) {
                JsonNode b64Node = root.get("data").get(0).get("b64_json");
                if (b64Node != null) {
                    return b64Node.asText();
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Base64 parse hatası: " + e.getMessage());
        }
        return null;
    }

    private String safePrompt(String prompt) {
        if (prompt == null) return "";
        String safePrefix = "This is a safe, family-friendly, artistic event poster. ";
        String cleaned = prompt
            .replaceAll("[\n\r]+", " ")
            .replaceAll("[ ]{2,}", " ")
            .replaceAll("[-–—]{2,}", "-")
            .replaceAll("[!]{2,}", "!")
            .replaceAll("[\"]{2,}", "\"")
            .replaceAll("[•·●]+", "")
            .replaceAll("[\uD83C-\uDBFF][\uDC00-\uDFFF]", "")
            .trim();
        return safePrefix + cleaned;
    }

    private String fallbackPrompt() {
        return "This is a safe, family-friendly, artistic event poster for Istanbul. Colorful, positive, modern, and visually appealing. No people, no violence, no politics.";
    }
} 