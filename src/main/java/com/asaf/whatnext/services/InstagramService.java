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
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
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

    private final PriorityBlockingQueue<PostTask> postQueue = new PriorityBlockingQueue<>();
    private final List<LocalDate> postedDates = new ArrayList<>();
    private final List<PostTask> failedTasks = new CopyOnWriteArrayList<>();

    private enum PostType {
        POST, STORY
    }

    private static class PostTask implements Comparable<PostTask> {
        final String id;
        final PostType type;
        final List<InstagramImage> images;
        final String caption;
        final InstagramImage storyImage;
        final LocalDate date;
        int retryCount = 0;
        private static final int MAX_RETRIES = 3;

        PostTask(List<InstagramImage> images, String caption, LocalDate date) {
            this.type = PostType.POST;
            this.images = images;
            this.caption = caption;
            this.storyImage = null;
            this.date = date;
            this.id = "POST-" + date.toString();
        }

        PostTask(InstagramImage storyImage, LocalDate date) {
            this.type = PostType.STORY;
            this.storyImage = storyImage;
            this.images = null;
            this.caption = null;
            this.date = date;
            this.id = "STORY-" + date.toString();
        }

        @Override
        public int compareTo(PostTask o) {
            return this.date.compareTo(o.date);
        }
    }

    @PostConstruct
    public void init() {
        if (accessToken == null || accessToken.isEmpty()) {
            String envToken = System.getenv("FACEBOOK_TOKEN");
            if (envToken != null && !envToken.isEmpty()) {
                accessToken = envToken;
                System.out.println("ℹ️ Instagram access token env'den yüklendi (FACEBOOK_TOKEN).");
            }
        }
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
        LocalDate date = images.get(0).getCreationDate();
        boolean alreadyQueued = postQueue.stream().anyMatch(task ->
            task.date.equals(date) && task.type == PostType.POST
        );
        boolean alreadyPosted = postedDates.contains(date) &&
            instagramImageRepository.existsByCreationDateAndPostTypeAndPostedToInstagramIsTrue(date, "cover");
        if (alreadyQueued || alreadyPosted) {
            System.err.println("❌ Aynı gün için zaten bir POST/COVER paylaşım kuyruğa alınmış veya paylaşılmış: " + date);
            return;
        }
        boolean added = postQueue.offer(new PostTask(images, caption, date));
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
        LocalDate date = image.getCreationDate();
        
        boolean alreadyPosted = instagramImageRepository.existsByCreationDateAndPostTypeAndPostedToInstagramIsTrue(date, "story");
        if (alreadyPosted) {
            System.err.println("❌ Aynı gün için zaten bir STORY paylaşılmış: " + date);
            return;
        }
        boolean added = postQueue.offer(new PostTask(image, date));
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
        return createContainerFromUrl(imageUrl, isStory, null);
    }

    private String createContainerFromUrl(String imageUrl, boolean isStory, String caption) throws IOException, InstagramApiException {
        String endpoint = GRAPH_API_BASE + userId + "/media";
        String postData = "image_url=" + URLEncoder.encode(imageUrl, StandardCharsets.UTF_8) + "&access_token=" + accessToken;
        if (isStory) {
            postData += "&media_type=STORIES";
        }
        if (caption != null && !caption.isEmpty()) {
            postData += "&caption=" + URLEncoder.encode(caption, StandardCharsets.UTF_8);
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

    
    private String getContainerStatus(String containerId) {
        try {
            String endpoint = GRAPH_API_BASE + containerId + "?fields=status_code&access_token=" + accessToken;
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String status = objectMapper.readTree(reader).get("status_code").asText();
                    return status;
                }
            } else {
                System.err.println("[Publisher] Container status sorgusu başarısız. Response code: " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("[Publisher] Container status sorgusunda hata: " + e.getMessage());
        }
        return "UNKNOWN";
    }

    private void postPublisher() {
        int rateLimitWaitMinutes = 15;
        while (true) {
            try {
                PostTask task = postQueue.take();
                System.out.println("[Publisher] İşlenecek görev: " + task.id + " | Gün: " + task.date + " | Kuyrukta kalan: " + postQueue.size());
                boolean success = false;
                boolean retrySuggested = false;
                String lastContainerId = null;
                String status = "-";
                try {
                    if (task.type == PostType.POST) {
                        lastContainerId = executeSharePost(task.images, task.caption);
                    } else {
                        lastContainerId = executeShareStory(task.storyImage);
                    }
                    if (lastContainerId != null) {
                        status = getContainerStatus(lastContainerId);
                        if ("PUBLISHED".equalsIgnoreCase(status)) {
                            success = true;
                        }
                    }
                    if (success) {
                        System.out.println("[Publisher] Görev BAŞARILI: " + task.id + " | Gün: " + task.date + " | ContainerID: " + lastContainerId + " | Status: " + status);
                        rateLimitWaitMinutes = 15;
                    } else {
                        System.err.println("[Publisher] Görev BAŞARISIZ: " + task.id + " | Gün: " + task.date + " | ContainerID: " + lastContainerId + " | Status: " + status);
                    }
                } catch (InstagramApiException e) {
                    if (lastContainerId != null) {
                        status = getContainerStatus(lastContainerId);
                        if ("PUBLISHED".equalsIgnoreCase(status)) {
                            success = true;
                            System.out.println("[Publisher] Görev BAŞARILI (API hata ama status PUBLISHED): " + task.id + " | Gün: " + task.date + " | ContainerID: " + lastContainerId + " | Status: " + status);
                        } else {
                            System.err.println("[Publisher] ❌ Instagram API hatası | Görev: " + task.id + " | Gün: " + task.date + " | ContainerID: " + lastContainerId + " | Status: " + status + " | " + e.getMessage());
                        }
                    } else {
                        System.err.println("[Publisher] ❌ Instagram API hatası | Görev: " + task.id + " | Gün: " + task.date + " | ContainerID: null | Status: - | " + e.getMessage());
                    }
                    if (e.isExpiredOrInvalidToken()) {
                        System.err.println("[Publisher] ❌ Access token geçersiz/expired. Kuyruk beklemeye alındı. Yeni token gereklidir.");
                        
                        Thread.sleep(1000L * 60 * 30);
                    } else if (e.isRateLimitError() || e.isActionBlockedError()) {
                        System.err.println("[Publisher] 🕒 API limit/engel hatası. " + rateLimitWaitMinutes + " dakika bekleniyor...");
                        Thread.sleep(1000L * 60 * rateLimitWaitMinutes);
                        retrySuggested = true;
                    }
                } catch (Exception e) {
                    System.err.println("[Publisher] ❌ Beklenmedik paylaşım hatası | Görev: " + task.id + " | Gün: " + task.date + " | ContainerID: " + lastContainerId + " | Status: " + status + " | " + e.getMessage());
                }

                if (retrySuggested) {
                    task.retryCount++;
                    if (task.retryCount >= PostTask.MAX_RETRIES) {
                        System.err.println("[Publisher] ❌ Görev maksimum deneme sayısına ulaştı: " + task.id + " | Deneme: " + task.retryCount);
                        failedTasks.add(task);
                        continue;
                    }
                    System.out.println("[Publisher] Görev tekrar deneme için yeniden kuyruğa alınıyor: " + task.id + " | Deneme: " + task.retryCount + "/" + PostTask.MAX_RETRIES);
                    postQueue.offer(task);
                    continue;
                }

                if (success) {
                    postedDates.add(task.date);
                } else {
                    failedTasks.add(task);
                }

                
                System.out.println("[Publisher] Görev sonrası 3 dakika bekleniyor...");
                Thread.sleep(180000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("❌ Instagram Publisher thread kesintiye uğradı.");
                break;
            }
        }
    }

    private void waitForContainerReady(String containerId) throws IOException, InterruptedException {
        int maxRetries = 30;
        int retryCount = 0;
        while (retryCount < maxRetries) {
            String status = getContainerStatus(containerId);
            if ("FINISHED".equalsIgnoreCase(status)) {
                System.out.println("✅ Container hazır: " + containerId);
                return;
            } else if ("ERROR".equalsIgnoreCase(status) || "EXPIRED".equalsIgnoreCase(status)) {
                throw new IOException("Container hatası veya süresi doldu. Status: " + status + " ID: " + containerId);
            }
            System.out.println("⏳ Container hazırlanıyor (" + status + ")... ID: " + containerId);
            Thread.sleep(2000);
            retryCount++;
        }
        throw new IOException("Container hazırlama zaman aşımı. ID: " + containerId);
    }

    private String executeSharePost(List<InstagramImage> images, String caption) throws IOException, InstagramApiException, InterruptedException {
        List<InstagramImage> imagesToPost = images.stream()
            .filter(img -> !img.isPostedToInstagram())
            .collect(java.util.stream.Collectors.toList());
        
        if (imagesToPost.isEmpty()) {
            throw new IOException("Paylaşılacak görsel bulunamadı.");
        }

        String containerId;
        if (imagesToPost.size() == 1) {
            InstagramImage singleImage = imagesToPost.get(0);
            containerId = createContainerFromUrl(singleImage.getCloudinaryUrl(), false, caption);
            if (containerId == null) throw new IOException("Single image container oluşturulamadı.");
            System.out.println("[Publisher] executeSharePost: Single image post | ContainerID: " + containerId);
        } else {
            if (imagesToPost.size() > 10) {
                System.out.println("⚠️ " + imagesToPost.size() + " görsel bulundu, Instagram carousel limiti 10 olduğu için ilk 10 görsel kullanılacak.");
                imagesToPost = imagesToPost.subList(0, 10);
            }
            
            List<String> containerIds = new ArrayList<>();
            for (InstagramImage image : imagesToPost) {
                String childContainerId = createContainerFromUrl(image.getCloudinaryUrl(), false);
                if (childContainerId != null) {
                    waitForContainerReady(childContainerId);
                    containerIds.add(childContainerId);
                }
            }
            if (containerIds.isEmpty()) throw new IOException("Hiçbir görsel yüklenemedi.");
            
            containerId = createCarouselContainer(containerIds, caption);
            if (containerId == null) throw new IOException("Carousel container oluşturulamadı.");
            System.out.println("[Publisher] executeSharePost: Carousel post | CarouselID: " + containerId + " | Görsel sayısı: " + containerIds.size());
        }
        
        waitForContainerReady(containerId);

        publishContainer(containerId);

        for (InstagramImage image : imagesToPost) {
            image.setPostedToInstagram(true);
        }
        instagramImageRepository.saveAll(imagesToPost);

        return containerId;
    }

    private String executeShareStory(InstagramImage image) throws IOException, InstagramApiException, InterruptedException {
         if(image.isPostedToInstagram()){
            System.out.println("ℹ️ Story zaten paylaşıldı olarak işaretlenmiş, atlanıyor: " + image.getId());
            return null;
        }
        String containerId = createContainerFromUrl(image.getCloudinaryUrl(), true);
        if (containerId == null) throw new IOException("Story container oluşturulamadı.");
        
        waitForContainerReady(containerId);
        
        publishContainer(containerId);

        image.setPostedToInstagram(true);
        instagramImageRepository.save(image);
        
        System.out.println("[Publisher] executeShareStory: ContainerID: " + containerId + " | Story paylaşıldı.");
        return containerId;
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

        public boolean isExpiredOrInvalidToken() {
            
            return errorBody.contains("\"code\": 190")
                || errorBody.contains("\"code\": 102")
                || errorBody.contains("\"error_subcode\": 463")
                || errorBody.contains("\"error_subcode\": 460")
                || errorBody.contains("\"error_subcode\": 467")
                || errorBody.toLowerCase().contains("invalid oauth")
                || errorBody.toLowerCase().contains("access token has expired")
                || errorBody.toLowerCase().contains("session has expired")
                || errorBody.toLowerCase().contains("invalid token");
        }
    }
} 