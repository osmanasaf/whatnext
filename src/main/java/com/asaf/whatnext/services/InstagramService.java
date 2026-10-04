package com.asaf.whatnext.services;

import com.asaf.whatnext.models.InstagramImage;
import com.asaf.whatnext.repository.InstagramImageRepository;
import com.asaf.whatnext.services.images.ImageRenderEngine;
import com.asaf.whatnext.services.images.ImageRenderRequest;
import com.fasterxml.jackson.databind.JsonNode;
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

    /**
     * Feed post, carousel ve Reel'e eklenen konum (Graph API {@code location_id} = konumun Facebook sayfa
     * ID'si). Boşsa konum eklenmez. Story'ler konum parametresini desteklemiyor.
     */
    @Value("${instagram.location-id:}")
    private String locationId;

    private String locationParam() {
        return locationId == null || locationId.isBlank() ? "" : "&location_id=" + locationId.trim();
    }

    @Autowired
    private InstagramImageRepository instagramImageRepository;
    @Autowired
    private ImageRenderEngine imageRenderEngine;
    @Autowired
    private CloudinaryService cloudinaryService;

    private static final String GRAPH_API_BASE = "https://graph.facebook.com/v19.0/";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final PriorityBlockingQueue<PostTask> postQueue = new PriorityBlockingQueue<>();
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
            imageFile = imageRenderEngine.render(new ImageRenderRequest(eventType, postType, city, date));

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
        // Aynı açılış görseli = aynı gönderi. Tarihe göre engellemek, aynı güne düşen günlük gönderi ile
        // karuseli (ör. Cuma günlüğü + Perşembe'nin Cuma tarihli hafta sonu karuseli) birbirine kilitliyordu.
        Long hookId = images.get(0).getId();
        boolean alreadyQueued = postQueue.stream().anyMatch(task ->
            task.type == PostType.POST && hookId != null && hookId.equals(task.images.get(0).getId())
        );
        if (alreadyQueued || images.get(0).isPostedToInstagram()) {
            System.err.println("❌ Bu gönderi zaten kuyrukta veya paylaşılmış: " + date + " (görsel " + hookId + ")");
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

    private static final int MEDIA_FETCH_ATTEMPTS = 4;
    private static final long MEDIA_FETCH_RETRY_MS = 20_000L;

    /**
     * Instagram'ın görseli indirmesi aralıklı başarısız oluyor (9004/2207052): 2026-09-15'te aynı carousel'in
     * 4 JPEG'inden 2'si container'a dönüştü, 3.'sü "Medya indirilemedi" ile düştü — adres elle hep 200/image/jpeg.
     * Tüm görevi düşürmek yerine yalnız o görsel kısa aralıkla yeniden denenir.
     */
    private String createContainerFromUrl(String imageUrl, boolean isStory, String caption) throws IOException, InstagramApiException {
        for (int attempt = 1; ; attempt++) {
            try {
                return createContainerOnce(imageUrl, isStory, caption);
            } catch (InstagramApiException e) {
                if (!e.isMediaFetchError() || attempt >= MEDIA_FETCH_ATTEMPTS) {
                    throw e;
                }
                System.err.println("[Publisher] Medya indirilemedi (9004), " + (MEDIA_FETCH_RETRY_MS / 1000)
                        + " sn sonra yeniden deneniyor (" + attempt + "/" + MEDIA_FETCH_ATTEMPTS + "): " + imageUrl);
                try {
                    Thread.sleep(MEDIA_FETCH_RETRY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    private String createContainerOnce(String imageUrl, boolean isStory, String caption) throws IOException, InstagramApiException {
        String endpoint = GRAPH_API_BASE + userId + "/media";
        // Görseller Cloudinary'de webp; Instagram image_url'de yalnızca JPEG'i destekliyor ("Only photo or video can be
        // accepted" reddi). Cloudinary uzantı değişince aynı görseli JPEG olarak sunar — eski kayıtlar da kapsanır.
        String jpegUrl = imageUrl.replaceFirst("\\.webp$", ".jpg");
        warmUp(jpegUrl);
        String postData = "image_url=" + URLEncoder.encode(jpegUrl, StandardCharsets.UTF_8) + "&access_token=" + accessToken;
        if (isStory) {
            postData += "&media_type=STORIES";
        }
        if (caption != null && !caption.isEmpty()) {
            // Caption'lı görsel = tek görselli feed post (carousel çocukları caption'sız gelir).
            postData += "&caption=" + URLEncoder.encode(caption, StandardCharsets.UTF_8) + (isStory ? "" : locationParam());
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

    /**
     * Instagram indirmeden önce adresi bir kez çeker. 2026-09-15: Instagram'ın 3 denemesi Cloudinary'nin
     * {@code .jpg} kopyasını hiç üretemedi ("Only photo or video can be accepted"); aynı adres elle çekilince
     * kopya oluştu ve 0,2 sn'de image/jpeg döndü. Eski webp kayıtları için; hata olursa sessizce geçilir.
     */
    private static void warmUp(String imageUrl) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(imageUrl).openConnection();
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(30_000);
            try (var in = conn.getInputStream()) {
                in.transferTo(OutputStream.nullOutputStream());
            }
        } catch (IOException ignored) {
            // Isıtma başarısızsa asıl hatayı Graph API cevabı gösterecek.
        }
    }

    private String createCarouselContainer(List<String> children, String caption) throws IOException, InstagramApiException {
        String endpoint = GRAPH_API_BASE + userId + "/media";
        String childrenIds = String.join(",", children);
        String postData = "media_type=CAROUSEL&children=" + childrenIds + "&caption=" + URLEncoder.encode(caption, StandardCharsets.UTF_8) + locationParam() + "&access_token=" + accessToken;
        
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
                        if (lastContainerId != null) {
                            status = getContainerStatus(lastContainerId);
                            if ("PUBLISHED".equalsIgnoreCase(status)) {
                                success = true;
                            }
                        }
                    } else {
                        lastContainerId = executeShareStory(task.storyImage);
                        // Story container'ları için publishContainer exception fırlatmazsa başarılıdır.
                        // getContainerStatus story container'larında güvenilir sonuç vermeyebilir.
                        if (lastContainerId != null) {
                            success = true;
                            status = "STORY_PUBLISHED";
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

                if (!success) {
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
        waitForContainer(containerId, 30, 2000);
    }

    private void waitForReelContainerReady(String containerId) throws IOException, InterruptedException {
        waitForContainer(containerId, 300, 2000);
    }

    private void waitForContainer(String containerId, int maxRetries, int sleepMs) throws IOException, InterruptedException {
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
            Thread.sleep(sleepMs);
            retryCount++;
        }
        throw new IOException("Container hazırlama zaman aşımı. ID: " + containerId);
    }

    private String executeSharePost(List<InstagramImage> images, String caption) throws IOException, InstagramApiException, InterruptedException {
        // Önceden paylaşılmış görseller ELENMEZ: haftalık/hafta sonu karuseli gün görsellerini bilerek yeniden
        // kullanır (2026-10-01: Cuma/Cumartesi elendi, karusel yalnız Pazar'la çıktı). Çift paylaşımı çağıranlar
        // önler — günlük akış yalnız paylaşılmamışları seçer, karusel kapağın paylaşılıp paylaşılmadığına bakar.
        List<InstagramImage> imagesToPost = new ArrayList<>(images);

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

    public void shareReel(String videoUrl, String caption) throws IOException, InstagramApiException, InterruptedException {
        String containerId = createReelContainer(videoUrl, caption);
        if (containerId == null) {
            throw new IOException("Reel container oluşturulamadı.");
        }
        waitForReelContainerReady(containerId);
        publishContainer(containerId);
        System.out.println("[Publisher] Reel paylaşıldı. ContainerID: " + containerId);
    }

    private String createReelContainer(String videoUrl, String caption) throws IOException, InstagramApiException {
        String endpoint = GRAPH_API_BASE + userId + "/media";
        String postData = "media_type=REELS"
            + "&video_url=" + URLEncoder.encode(videoUrl, StandardCharsets.UTF_8)
            // Kapak karesi: giriş ekranı (afiş mozaiği + ay renkli panel) tam oturmuş hâlde. Verilmezse
            // Instagram ilk kareyi alıyor — orada afişler/yazı henüz görünmüyor (bkz. video/src/timing.ts HOOK_FRAMES).
            + "&thumb_offset=1200"
            + locationParam()
            + "&access_token=" + accessToken;
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

    public String getAccessToken() {
        return accessToken;
    }

    public String getUserId() {
        return userId;
    }

    /**
     * Graph API hata gövdesi {@code {"error":{"code":..,"error_subcode":..}}} — kodlar tam sayı olarak okunur.
     * Önceden {@code contains("\"code\":9")} ile bakılıyordu; bu {@code "code":9004} (medya indirilemedi,
     * ör. "Only photo or video can be accepted") gövdesini de rate limit sayıp her görevi 15 dk uyutup
     * 3 kez yeniden deniyordu — tek iş parçacıklı kuyruk saatlerce tıkanıyordu (2026-09-15).
     */
    public static class InstagramApiException extends Exception {
        private static final ObjectMapper ERROR_MAPPER = new ObjectMapper();

        private final String errorBody;
        private final int responseCode;
        private final int code;
        private final int subcode;

        public InstagramApiException(String errorBody, int responseCode) {
            this(errorBody == null ? "" : errorBody, responseCode, parseError(errorBody));
        }

        private InstagramApiException(String errorBody, int responseCode, int[] codeAndSubcode) {
            // Mesajda kod/alt kod/açıklama: Publisher satırı tek başına teşhis için yeterli olsun
            // (Coolify log ekranı çok satırlı çıktıda satır kaybedebiliyor).
            super("Instagram API returned error code " + responseCode
                    + " (code=" + codeAndSubcode[0] + ", subcode=" + codeAndSubcode[1] + ") "
                    + errorBody.substring(0, Math.min(errorBody.length(), 300)));
            this.errorBody = errorBody;
            this.responseCode = responseCode;
            this.code = codeAndSubcode[0];
            this.subcode = codeAndSubcode[1];
        }

        private static int[] parseError(String body) {
            try {
                JsonNode error = ERROR_MAPPER.readTree(body == null ? "" : body).path("error");
                return new int[]{error.path("code").asInt(-1), error.path("error_subcode").asInt(-1)};
            } catch (Exception e) {
                return new int[]{-1, -1};
            }
        }

        /** 4/17/32/613 istek limitleri; 9 (ör. alt kod 2207042: 24 saatlik gönderi limiti). 9004 DEĞİL. */
        public boolean isRateLimitError() {
            return code == 4 || code == 9 || code == 17 || code == 32 || code == 613
                || errorBody.toLowerCase().contains("request limit reached");
        }

        public boolean isActionBlockedError() {
            return subcode == 2207051;
        }

        /** Instagram medya adresini indiremedi ("Only photo or video can be accepted") — aralıklı, yeniden denenebilir. */
        public boolean isMediaFetchError() {
            return code == 9004 || subcode == 2207052;
        }

        public boolean isExpiredOrInvalidToken() {
            return code == 190 || code == 102
                || subcode == 460 || subcode == 463 || subcode == 467
                || errorBody.toLowerCase().contains("invalid oauth")
                || errorBody.toLowerCase().contains("access token has expired")
                || errorBody.toLowerCase().contains("session has expired")
                || errorBody.toLowerCase().contains("invalid token");
        }
    }
} 