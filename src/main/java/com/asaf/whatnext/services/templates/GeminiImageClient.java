package com.asaf.whatnext.services.templates;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Google Gemini image REST istemcisi (Anthropic'te görsel üretim API'si yok, plan gereği Gemini varsayılan).
 * {@code whatnext.templates.ai.api-key} boşsa hemen hata verir — {@link AiTemplateProvider} bunu
 * yakalayıp {@link GeneratedTemplateProvider}'a düşer, 11:50 cron'u asla bloklanmaz.
 */
@Component
public class GeminiImageClient implements AiImageClient {

    private static final String ENDPOINT_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey;
    private final String model;

    public GeminiImageClient(
            RestTemplate restTemplate,
            @Value("${whatnext.templates.ai.api-key:}") String apiKey,
            @Value("${whatnext.templates.ai.model:gemini-2.0-flash-preview-image-generation}") String model) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public byte[] generateImage(String prompt, int width, int height) throws IOException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IOException("whatnext.templates.ai.api-key ayarlanmamış");
        }

        String url = String.format(ENDPOINT_TEMPLATE, model, apiKey);
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("responseModalities", List.of("TEXT", "IMAGE")));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
        } catch (Exception e) {
            throw new IOException("Gemini isteği başarısız: " + e.getMessage(), e);
        }

        JsonNode root = mapper.readTree(response.getBody());
        JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
        for (JsonNode part : parts) {
            JsonNode inline = part.path("inlineData");
            if (!inline.isMissingNode() && inline.has("data")) {
                return Base64.getDecoder().decode(inline.path("data").asText());
            }
        }
        throw new IOException("Gemini yanıtında görsel (inlineData) bulunamadı");
    }
}
