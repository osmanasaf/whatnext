package com.asaf.whatnext.services;

import com.asaf.whatnext.models.InstagramImage;
import com.asaf.whatnext.repository.InstagramImageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class InstagramService {

    @Value("${instagram.access.token}")
    private String accessToken;

    @Value("${instagram.user.id}")
    private String userId;

    @Autowired
    private InstagramImageRepository instagramImageRepository;
    @Autowired
    private ImageGenerationService imageGenerationService;
    @Autowired
    private CloudinaryService cloudinaryService;

    private static final String GRAPH_API_BASE = "https://graph.facebook.com/v19.0/";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int RETRY_COUNT = 3;
    private static final int RETRY_WAIT_MS = 2000;



    private final BlockingQueue<Runnable> failedQueue = new LinkedBlockingQueue<>();
    
    public InstagramImage createImage(String eventType, String postType, String city, LocalDate date) throws IOException, InterruptedException {
        File imageFile = null;
        try {
            if ("cover".equals(postType)) {
                imageFile = imageGenerationService.createCover(date);
            } else if ("story".equals(postType)) {
                imageFile = imageGenerationService.createSmartStory(date);
            } else {
                imageFile = imageGenerationService.createPost(eventType, postType, city, date);
            }

            if (imageFile == null) {
                throw new IOException("Görsel dosyası oluşturulamadı.");
            }

            return uploadAndSaveImage(imageFile, postType, eventType, date);
        } finally {
            if (imageFile != null && imageFile.exists()) {
                try {
                    Files.delete(imageFile.toPath());
                } catch (IOException e) {
                    System.err.println("❌ Geçici dosya silinemedi: " + imageFile.getAbsolutePath() + " - Hata: " + e.getMessage());
                }
            }
        }
    }

    public void sharePost(List<InstagramImage> images, String caption) {
        Runnable task = () -> {
            for (int attempt = 1; attempt <= RETRY_COUNT; attempt++) {
                try {
                    List<String> containerIds = new ArrayList<>();
                    for (InstagramImage image : images) {
                        String containerId = createContainerFromUrl(image.getCloudinaryUrl(), false);
                        if (containerId != null) {
                            containerIds.add(containerId);
                        }
                    }
                    if (containerIds.isEmpty()) throw new IOException("Hiçbir görsel yüklenemedi.");
                    String carouselId = createCarouselContainer(containerIds, caption);
                    if (carouselId == null) throw new IOException("Carousel container oluşturulamadı.");
                    publishContainer(carouselId);

                    images.forEach(image -> {
                        image.setPostedToInstagram(true);
                        instagramImageRepository.save(image);
                    });

                    System.out.println("✅ Instagram post paylaşımı başarılı.");
                    return;
                } catch (Exception e) {
                    System.err.println("❌ Instagram post paylaşımı hatası (" + attempt + "): " + e.getMessage());
                    if (attempt < RETRY_COUNT) {
                        try { Thread.sleep(RETRY_WAIT_MS); } catch (InterruptedException ignored) {}
                    } else {
                        System.err.println("❌ Post paylaşımı " + RETRY_COUNT + " deneme sonunda başarısız oldu. Kuyruğa ekleniyor.");
                        failedQueue.offer(() -> sharePost(images, caption));
                    }
                }
            }
        };
        new Thread(task).start();
    }

    public void shareStory(InstagramImage image) {
        Runnable task = () -> {
            for (int attempt = 1; attempt <= RETRY_COUNT; attempt++) {
                try {
                    String containerId = createContainerFromUrl(image.getCloudinaryUrl(), true);
                    if (containerId == null) throw new IOException("Story container oluşturulamadı.");
                    publishContainer(containerId);

                    image.setPostedToInstagram(true);
                    instagramImageRepository.save(image);
                    
                    System.out.println("✅ Instagram story paylaşımı başarılı.");
                    return;
                } catch (Exception e) {
                    System.err.println("❌ Instagram story paylaşımı hatası (" + attempt + "): " + e.getMessage());
                    if (attempt < RETRY_COUNT) {
                        try { Thread.sleep(RETRY_WAIT_MS); } catch (InterruptedException ignored) {}
                    } else {
                        System.err.println("❌ Story paylaşımı " + RETRY_COUNT + " deneme sonunda başarısız oldu. Kuyruğa ekleniyor.");
                        failedQueue.offer(() -> shareStory(image));
                    }
                }
            }
        };
        new Thread(task).start();
    }
    
    private InstagramImage uploadAndSaveImage(File imageFile, String postType, String eventType, LocalDate date) throws IOException {
        String folder = "whatnext/" + postType + "s/" + date.toString();
        Map<String, Object> uploadResult = cloudinaryService.uploadImage(imageFile, folder);

        String url = (String) uploadResult.get("secure_url");
        String publicId = (String) uploadResult.get("public_id");

        InstagramImage instagramImage = new InstagramImage();
        instagramImage.setCloudinaryUrl(url);
        instagramImage.setCloudinaryPublicId(publicId);
        instagramImage.setPostType(postType);
        instagramImage.setEventType(eventType);
        instagramImage.setCreationDate(date);
        instagramImage.setPostedToInstagram(false);

        return instagramImageRepository.save(instagramImage);
    }

    private String createContainerFromUrl(String imageUrl, boolean isStory) throws IOException {
        String endpoint = GRAPH_API_BASE + userId + "/media";
        String postData = "image_url=" + URLEncoder.encode(imageUrl, StandardCharsets.UTF_8) + "&access_token=" + accessToken;
        if (isStory) {
            postData += "&media_type=STORIES";
        }
        
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Length", Integer.toString(postData.getBytes(StandardCharsets.UTF_8).length));
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(postData.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                return objectMapper.readTree(reader).get("id").asText();
            }
        } else {
            handleApiError(conn);
            return null;
        }
    }

    private String createCarouselContainer(List<String> children, String caption) throws IOException {
        String endpoint = GRAPH_API_BASE + userId + "/media";
        String childrenIds = String.join(",", children);
        String postData = "media_type=CAROUSEL&children=" + childrenIds + "&caption=" + URLEncoder.encode(caption, StandardCharsets.UTF_8) + "&access_token=" + accessToken;
        
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Length", Integer.toString(postData.getBytes(StandardCharsets.UTF_8).length));
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(postData.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
             try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                return objectMapper.readTree(reader).get("id").asText();
            }
        } else {
            handleApiError(conn);
            return null;
        }
    }

    private void publishContainer(String containerId) throws IOException {
        String endpoint = GRAPH_API_BASE + userId + "/media_publish";
        String postData = "creation_id=" + containerId + "&access_token=" + accessToken;

        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Length", Integer.toString(postData.getBytes(StandardCharsets.UTF_8).length));
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(postData.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            handleApiError(conn);
            throw new IOException("Failed to publish container. Response code: " + responseCode);
        }
    }

    private void handleApiError(HttpURLConnection conn) throws IOException {
        int responseCode = conn.getResponseCode();
        System.err.println("❌ Instagram API Error: " + responseCode);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
            StringBuilder errorResponse = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                errorResponse.append(line);
            }
            System.err.println("❌ Instagram API Error Body: " + errorResponse.toString());
        } catch (Exception ex) {
            System.err.println("❌ Error reading error stream: " + ex.getMessage());
        }
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getUserId() {
        return userId;
    }
} 