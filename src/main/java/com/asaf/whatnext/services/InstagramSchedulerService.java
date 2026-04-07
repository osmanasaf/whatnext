package com.asaf.whatnext.services;

import com.asaf.whatnext.models.InstagramImage;
import com.asaf.whatnext.repository.InstagramImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.ArrayList;

@Service
public class InstagramSchedulerService {

    @Autowired
    private InstagramService instagramService;

    @Autowired
    private InstagramImageRepository instagramImageRepository;

    @Autowired
    private WeeklyVideoService weeklyVideoService;
    private static final DateTimeFormatter TR_DATE_YEAR_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("tr"));

    private static final String DAILY_CAPTION_TEMPLATE = """
        📅 %s İstanbul Etkinlikleri 🎫
        
        🎭 Tiyatro | 🎶 Konser | 🎤 Stand-up
        Bu günlerde şehirde neler var? Kaçırma!
        
        🔗 Detaylar ve biletler profilde
        #istanbul #istanbuletkinlikleri #etkinlik #konser #tiyatro #sanat #istanbullife #istanbuldayasam #igersistanbul #istanbulnightlife #istanbulbosphorus #galatatower
        """;

    /** Aynı gün içinde arka arkaya postlar arası (Instagram kuyruğu / limit) */
    private static final long DELAY_BETWEEN_DAILY_POSTS_MS = 90_000L;



    @Scheduled(cron = "0 0 13 ? * TUE", zone = "Europe/Istanbul")
    public void generateAndShareWeeklyReel() {
        System.out.println("[Scheduler] Haftalık Reel video oluşturma ve paylaşım görevi başlatıldı.");
        try {
            weeklyVideoService.generateAndShareWeeklyReel();
            System.out.println("[Scheduler] Haftalık Reel görevi tamamlandı.");
        } catch (Exception e) {
            System.err.println("[Scheduler] Haftalık Reel hatası: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Scheduled(cron = "0 50 11 * * ?", zone = "Europe/Istanbul")
    @Transactional
    public void createDailyStory() {
        System.out.println("[Scheduler] Günlük story görseli oluşturma ve kaydetme görevi başlatıldı.");
        try {
            LocalDate today = LocalDate.now();
            boolean exists = instagramImageRepository.findByPostTypeAndEventTypeAndCreationDate("story", "smart_story", today).isPresent();
            if (!exists) {
                InstagramImage storyImage = instagramService.createImage( "smart_story", "story", null, today);
                if (storyImage != null) {
                    System.out.println("[Scheduler] Günlük story görseli başarıyla oluşturuldu ve veritabanına kaydedildi.");
                } else {
                    System.out.println("[Scheduler] Bugün için hiç etkinlik bulunamadı. Story görseli oluşturulmadı.");
                }
            } else {
                System.out.println("[Scheduler] Bugünün story görseli zaten mevcut, oluşturma işlemi atlandı.");
            }
        } catch (Exception e) {
            System.err.println("[Scheduler] Story oluşturma hatası: " + e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 12 * * ?", zone = "Europe/Istanbul")
    @Transactional
    public void shareDailyStoryToInstagram() {
        System.out.println("[Scheduler] Günlük story paylaşma görevi başlatıldı.");
        try {
            LocalDate today = LocalDate.now();
            if (instagramImageRepository.existsByCreationDateAndPostTypeAndPostedToInstagramIsTrue(today, "story")) {
                System.out.println("[Scheduler] Bugün için zaten bir story paylaşılmış. İşlem atlanıyor.");
                return;
            }

            Optional<InstagramImage> storyToShare = instagramImageRepository
                .findByPostTypeAndEventTypeAndCreationDate("story", "smart_story", today);

            // Görsel henüz oluşturulmamışsa burada oluştur
            if (storyToShare.isEmpty()) {
                System.out.println("[Scheduler] Bugün için story görseli bulunamadı, şimdi oluşturuluyor...");
                InstagramImage created = instagramService.createImage("smart_story", "story", null, today);
                if (created != null) {
                    storyToShare = Optional.of(created);
                    System.out.println("[Scheduler] Story görseli oluşturuldu, paylaşılacak.");
                } else {
                    System.out.println("[Scheduler] Bugün için etkinlik bulunamadı, story görseli üretilemedi.");
                    return;
                }
            }

            if (!storyToShare.get().isPostedToInstagram()) {
                instagramService.shareStory(storyToShare.get());
                System.out.println("[Scheduler] Instagram story paylaşım isteği gönderildi.");
            }
        } catch (Exception e) {
            System.err.println("[Scheduler] Instagram story paylaşımı sırasında hata: " + e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 16 ? * SUN", zone = "Europe/Istanbul")
    public void createWeeklyPosts() {
        System.out.println("[Scheduler] Haftalık post görselleri oluşturuluyor...");
        LocalDate today = LocalDate.now();
        LocalDate nextMonday = today.plusDays((8 - today.getDayOfWeek().getValue()) % 7);
        for (int i = 0; i < 7; i++) {
            LocalDate targetDate = nextMonday.plusDays(i);
            try {
                System.out.println("[Scheduler] " + targetDate + " için görseller oluşturuluyor...");
                
                InstagramImage coverImage = instagramService.createImage("cover", "cover", null, targetDate);
                if (coverImage != null) {
                    System.out.println("[Scheduler] " + targetDate + " için cover görseli oluşturuldu.");
                }
                
                InstagramImage concertImage = instagramService.createImage("concert", "post", "istanbul", targetDate);
                if (concertImage != null) {
                    System.out.println("[Scheduler] " + targetDate + " için konser görseli oluşturuldu.");
                }
                
                InstagramImage theaterImage = instagramService.createImage("theater", "post", "istanbul", targetDate);
                if (theaterImage != null) {
                    System.out.println("[Scheduler] " + targetDate + " için tiyatro görseli oluşturuldu.");
                }
                
                InstagramImage standupImage = instagramService.createImage("standup", "post", "istanbul", targetDate);
                if (standupImage != null) {
                    System.out.println("[Scheduler] " + targetDate + " için stand-up görseli oluşturuldu.");
                }
            } catch (Exception e) {
                System.err.println("[Scheduler] " + targetDate + " için görsel oluşturma hatası: " + e.getMessage());
                e.printStackTrace();
            }
        }
        System.out.println("[Scheduler] Haftalık post görselleri oluşturma işlemi tamamlandı.");
    }

    /**
     * Haftanın Pazartesi (Pazar createWeeklyPosts ile aynı hafta).
     * Pazar günü: yarınki Pazartesi; diğer günler: içinde bulunulan haftanın Pazartesi.
     */
    static LocalDate resolveFeedWeekMonday(LocalDate today) {
        if (today.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return today.plusDays(1);
        }
        return today.with(DayOfWeek.MONDAY);
    }

    /**
     * Pazar 17:00 — Pazartesi, Salı, Çarşamba (her biri ayrı post).
     */
    @Scheduled(cron = "0 0 17 ? * SUN", zone = "Europe/Istanbul")
    public List<String> shareWeeklyPostsSunday() throws InterruptedException {
        LocalDate monday = resolveFeedWeekMonday(LocalDate.now());
        return sharePostsForDatesSequential(
                List.of(monday, monday.plusDays(1), monday.plusDays(2)), "Pazar (Pzt-Çar)");
    }

    /**
     * Pazartesi 10:00 — Perşembe, Cuma, Cumartesi (her biri ayrı post).
     */
    @Scheduled(cron = "0 0 10 ? * MON", zone = "Europe/Istanbul")
    public List<String> shareWeeklyPostsMonday() throws InterruptedException {
        LocalDate monday = resolveFeedWeekMonday(LocalDate.now());
        return sharePostsForDatesSequential(
                List.of(monday.plusDays(3), monday.plusDays(4), monday.plusDays(5)), "Pazartesi (Per-Cum-Cts)");
    }

    /**
     * Salı 10:00 — Haftanın Pazar günü (ayrı post). Reel öncesi feed tamamlanır.
     */
    @Scheduled(cron = "0 0 10 ? * TUE", zone = "Europe/Istanbul")
    public List<String> shareWeeklyPostsTuesday() throws InterruptedException {
        LocalDate monday = resolveFeedWeekMonday(LocalDate.now());
        return sharePostsForDatesSequential(List.of(monday.plusDays(6)), "Salı (Paz)");
    }

    /**
     * Manuel tetikleme: ilgili haftanın 7 günü için her gün ayrı post (sırayla).
     */
    public List<String> shareFullWeekFeedPosts() throws InterruptedException {
        LocalDate monday = resolveFeedWeekMonday(LocalDate.now());
        List<LocalDate> week = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            week.add(monday.plusDays(i));
        }
        return sharePostsForDatesSequential(week, "Manuel (7 gün)");
    }

    private List<String> sharePostsForDatesSequential(List<LocalDate> dates, String batchName) throws InterruptedException {
        List<String> results = new ArrayList<>();
        System.out.println("[Scheduler] " + batchName + " — sırayla " + dates.size() + " günlük post...");
        for (int i = 0; i < dates.size(); i++) {
            results.addAll(sharePostsForSingleDate(dates.get(i), batchName + " [" + (i + 1) + "/" + dates.size() + "]"));
            if (i < dates.size() - 1) {
                Thread.sleep(DELAY_BETWEEN_DAILY_POSTS_MS);
            }
        }
        return results;
    }

    private List<String> sharePostsForSingleDate(LocalDate targetDate, String groupName) throws InterruptedException {
        List<String> results = new ArrayList<>();
        System.out.println("[Scheduler] " + groupName + " — " + targetDate + " için tek post gönderiliyor...");

        boolean coverShared = instagramImageRepository
                .existsByCreationDateAndPostTypeAndPostedToInstagramIsTrue(targetDate, "cover");
        if (coverShared) {
            String msg = "[Scheduler] " + targetDate + " için zaten cover paylaşılmış, atlanıyor.";
            System.out.println(msg);
            results.add(msg);
            return results;
        }

        List<InstagramImage> imagesToShare = instagramImageRepository
                .findByCreationDateAndPostedToInstagramFalse(targetDate);

        Map<String, InstagramImage> uniqueImages = new HashMap<>();
        for (InstagramImage img : imagesToShare) {
            if ("post".equals(img.getPostType()) || "cover".equals(img.getPostType())) {
                if (!uniqueImages.containsKey(img.getEventType())
                        || img.getId() > uniqueImages.get(img.getEventType()).getId()) {
                    uniqueImages.put(img.getEventType(), img);
                }
            }
        }
        List<InstagramImage> allPosts = new ArrayList<>(uniqueImages.values());

        if (allPosts.isEmpty()) {
            String msg = "[Scheduler] " + targetDate + " için paylaşılacak görsel yok.";
            System.out.println(msg);
            results.add(msg);
            return results;
        }

        allPosts.sort((img1, img2) -> {
            if ("cover".equals(img1.getEventType()) && !"cover".equals(img2.getEventType())) {
                return -1;
            }
            if (!"cover".equals(img1.getEventType()) && "cover".equals(img2.getEventType())) {
                return 1;
            }
            return Long.compare(img1.getId(), img2.getId());
        });

        String dateRange = targetDate.format(TR_DATE_YEAR_FORMAT);
        String caption = String.format(DAILY_CAPTION_TEMPLATE, dateRange);

        System.out.println("[Scheduler] " + groupName + " paylaşım başlatılıyor. Görsel ID'leri: "
                + allPosts.stream().map(InstagramImage::getId).toList());

        try {
            instagramService.sharePost(allPosts, caption);

            int maxAttempts = 30;
            boolean isPublished = false;
            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                Thread.sleep(10000);
                boolean checkShared = instagramImageRepository
                        .existsByCreationDateAndPostTypeAndPostedToInstagramIsTrue(targetDate, "cover");
                if (checkShared) {
                    isPublished = true;
                    break;
                }
                System.out.println("[Scheduler] " + targetDate + " paylaşımı bekleniyor... Deneme: "
                        + (attempt + 1) + "/" + maxAttempts);
            }

            if (isPublished) {
                String msg = "[Scheduler] " + targetDate + " paylaşımı tamamlandı.";
                System.out.println(msg);
                results.add(msg);
            } else {
                String msg = "[Scheduler] " + targetDate + " paylaşımı zaman aşımına uğradı veya başarısız oldu.";
                System.err.println(msg);
                results.add(msg);
            }
        } catch (Exception ex) {
            String err = "[Scheduler] HATA! " + targetDate + " paylaşımı: " + ex.getMessage();
            System.err.println(err);
            ex.printStackTrace();
            results.add(err);
        }

        return results;
    }
} 