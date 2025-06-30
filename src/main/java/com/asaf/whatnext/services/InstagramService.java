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
import jakarta.annotation.PostConstruct;

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
    private static final int RETRY_WAIT_MS = 5000; // 5 saniye
    private static final int API_LIMIT_WAIT_MS = 1000 * 60 * 15; // 15 dakika

    private final BlockingQueue<PostTask> postQueue = new LinkedBlockingQueue<>();

    private enum PostType {
        POST, STORY
    }

    private static class PostTask {
        final PostType type;
        final List<InstagramImage> images;
        final String caption;
        final InstagramImage storyImage;

        PostTask(List<InstagramImage> images, String caption) {
            this.type = PostType.POST;
            this.images = images;
            this.caption = caption;
            this.storyImage = null;
        }

        PostTask(InstagramImage storyImage) {
            this.type = PostType.STORY;
            this.storyImage = storyImage;
            this.images = null;
            this.caption = null;
        }
    }

    @PostConstruct
    public void init() {
        Thread publisherThread = new Thread(this::postPublisher);
        publisherThread.setDaemon(true);
        publisherThread.start();
        System.out.println("✅ Instagram Publisher thread started.");
    }
    
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
                System.out.println("⚠️ Görsel üretilemedi. InstagramImage oluşturulmayacak.");
                return null;
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
        if (images == null || images.isEmpty()) {
            System.err.println("❌ Paylaşılacak görsel bulunmadığı için post görevi kuyruğa eklenmedi.");
            return;
        }
        boolean added = postQueue.offer(new PostTask(images, caption));
        if(added) {
            System.out.println("✅ Post görevi kuyruğa eklendi. Kuyruktaki görev sayısı: " + postQueue.size());
        } else {
            System.err.println("❌ Post görevi kuyruğa eklenemedi.");
        }
    }

    public void shareStory(InstagramImage image) {
        if (image == null) {
            System.err.println("❌ Paylaşılacak görsel bulunmadığı için story görevi kuyruğa eklenmedi.");
            return;
        }
        boolean added = postQueue.offer(new PostTask(image));
         if(added) {
            System.out.println("✅ Story görevi kuyruğa eklendi. Kuyruktaki görev sayısı: " + postQueue.size());
        } else {
            System.err.println("❌ Story görevi kuyruğa eklenemedi.");
        }
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

    private String createContainerFromUrl(String imageUrl, boolean isStory) throws IOException, InstagramApiException {
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

    private String createCarouselContainer(List<String> children, String caption) throws IOException, InstagramApiException {
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

    private void publishContainer(String containerId) throws IOException, InstagramApiException {
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

    private void handleApiError(HttpURLConnection conn) throws IOException, InstagramApiException {
        int responseCode = conn.getResponseCode();
        StringBuilder errorResponse = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                errorResponse.append(line);
            }
        } catch (Exception ex) {
            System.err.println("❌ Error reading error stream: " + ex.getMessage());
        }
        
        String errorBody = errorResponse.toString();
        System.err.println("❌ Instagram API Error: " + responseCode);
        System.err.println("❌ Instagram API Error Body: " + errorBody);

        throw new InstagramApiException(errorBody, responseCode);
    }

    private void postPublisher() {
        while (true) {
            try {
                Thread.sleep(10000);
                PostTask task = postQueue.take();
                System.out.println("Processing task from queue. Remaining: " + postQueue.size());
                boolean success = false;
                for (int attempt = 1; attempt <= RETRY_COUNT; attempt++) {
                    try {
                        if (task.type == PostType.POST) {
                            executeSharePost(task.images, task.caption);
                        } else {
                            executeShareStory(task.storyImage);
                        }
                        success = true;
                        break; 
                    } catch (InstagramApiException e) {
                        System.err.println("❌ Instagram API hatası (" + attempt + "): " + e.getMessage());
                        if (e.isRateLimitError() || e.isActionBlockedError()) {
                             System.err.println("🕒 API limit veya engel hatası. " + (API_LIMIT_WAIT_MS / 60000) + " dakika bekleniyor...");
                             Thread.sleep(API_LIMIT_WAIT_MS);
                        } else if (attempt < RETRY_COUNT) {
                            Thread.sleep(RETRY_WAIT_MS * attempt);
                        }
                    } catch (Exception e) {
                        System.err.println("❌ Beklenmedik paylaşım hatası (" + attempt + "): " + e.getMessage());
                        if (attempt < RETRY_COUNT) {
                            Thread.sleep(RETRY_WAIT_MS * attempt);
                        }
                    }
                }

                if (!success) {
                    System.err.println("❌ Görev " + RETRY_COUNT + " deneme sonunda kalıcı olarak başarısız oldu.");
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("❌ Instagram Publisher thread kesintiye uğradı.");
                break;
            }
        }
    }

    private void executeSharePost(List<InstagramImage> images, String caption) throws IOException, InstagramApiException {
        List<String> containerIds = new ArrayList<>();
        for (InstagramImage image : images) {
            // Bir önceki paylaşım denemesinde zaten yüklendiyse tekrar yükleme.
            if(image.isPostedToInstagram()){
                System.out.println("ℹ️ Görsel zaten paylaşıldı olarak işaretlenmiş, atlanıyor: " + image.getId());
                continue;
            }
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
    }

    private void executeShareStory(InstagramImage image) throws IOException, InstagramApiException {
         if(image.isPostedToInstagram()){
            System.out.println("ℹ️ Story zaten paylaşıldı olarak işaretlenmiş, atlanıyor: " + image.getId());
            return;
        }
        String containerId = createContainerFromUrl(image.getCloudinaryUrl(), true);
        if (containerId == null) throw new IOException("Story container oluşturulamadı.");
        
        publishContainer(containerId);

        image.setPostedToInstagram(true);
        instagramImageRepository.save(image);
        
        System.out.println("✅ Instagram story paylaşımı başarılı.");
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getUserId() {
        return userId;
    }

    public static class InstagramApiException extends Exception {
        private final String errorBody;
        private final int responseCode;

        public InstagramApiException(String errorBody, int responseCode) {
            super("Instagram API returned error code " + responseCode);
            this.errorBody = errorBody;
            this.responseCode = responseCode;
        }

        public boolean isRateLimitError() {
            return errorBody.contains("\"code\": 4,") || errorBody.contains("\"code\": 9,") || errorBody.contains("request limit reached");
        }

        public boolean isActionBlockedError() {
            return errorBody.contains("\"error_subcode\": 2207051");
        }
    }
} 