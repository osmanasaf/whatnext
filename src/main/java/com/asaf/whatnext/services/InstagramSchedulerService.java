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
    private static final DateTimeFormatter TR_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM", new Locale("tr"));
    private static final DateTimeFormatter TR_DATE_YEAR_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("tr"));

    private static final String DAILY_CAPTION_TEMPLATE = """
        📅 %s İstanbul Etkinlikleri 🎫
        
        🎭 Tiyatro | 🎶 Konser | 🎤 Stand-up
        Bu günlerde şehirde neler var? Kaçırma!
        
        🔗 Detaylar ve biletler profilde
        #istanbul #istanbuletkinlikleri #etkinlik #konser #tiyatro #sanat #istanbullife #istanbuldayasam #igersistanbul #istanbulnightlife #istanbulbosphorus #galatatower
        """;



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
     * Pazar 17:00 — Pazartesi + Salı etkinlik postlarını paylaş
     */
    @Scheduled(cron = "0 0 17 ? * SUN", zone = "Europe/Istanbul")
    @Transactional
    public List<String> shareWeeklyPostsGroup1() throws InterruptedException {
        LocalDate today = LocalDate.now();
        LocalDate nextMonday = today.with(DayOfWeek.MONDAY).isAfter(today)
                ? today.with(DayOfWeek.MONDAY)
                : today.plusWeeks(1).with(DayOfWeek.MONDAY);
        List<LocalDate> dates = List.of(nextMonday, nextMonday.plusDays(1));
        return sharePostsForDates(dates, "Grup 1 (Pzt-Sal)");
    }

    /**
     * Salı 10:00 — Çarşamba + Perşembe + Cuma etkinlik postlarını paylaş
     */
    @Scheduled(cron = "0 0 10 ? * TUE", zone = "Europe/Istanbul")
    @Transactional
    public List<String> shareWeeklyPostsGroup2() throws InterruptedException {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        List<LocalDate> dates = List.of(monday.plusDays(2), monday.plusDays(3), monday.plusDays(4));
        return sharePostsForDates(dates, "Grup 2 (Çar-Per-Cum)");
    }

    /**
     * Çarşamba 10:00 — Cumartesi + Pazar etkinlik postlarını paylaş
     */
    @Scheduled(cron = "0 0 10 ? * WED", zone = "Europe/Istanbul")
    @Transactional
    public List<String> shareWeeklyPostsGroup3() throws InterruptedException {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        List<LocalDate> dates = List.of(monday.plusDays(5), monday.plusDays(6));
        return sharePostsForDates(dates, "Grup 3 (Cts-Paz)");
    }

    private List<String> sharePostsForDates(List<LocalDate> dates, String groupName) throws InterruptedException {
        List<String> results = new ArrayList<>();
        System.out.println("[Scheduler] " + groupName + " postları Instagram'a gönderiliyor...");

        List<InstagramImage> allPosts = new ArrayList<>();
        for (LocalDate targetDate : dates) {
            boolean coverShared = instagramImageRepository
                    .existsByCreationDateAndPostTypeAndPostedToInstagramIsTrue(targetDate, "cover");
            if (coverShared) {
                String msg = "[Scheduler] " + targetDate + " için zaten bir cover paylaşılmış, atlanıyor.";
                System.out.println(msg);
                results.add(msg);
                continue;
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
            allPosts.addAll(uniqueImages.values());
        }

        if (allPosts.isEmpty()) {
            String msg = "[Scheduler] " + groupName + " için paylaşılacak görsel bulunamadı.";
            System.out.println(msg);
            results.add(msg);
            return results;
        }

        allPosts.sort((img1, img2) -> {
            int dateCompare = img1.getCreationDate().compareTo(img2.getCreationDate());
            if (dateCompare != 0) return dateCompare;
            return "cover".equals(img1.getEventType()) ? -1 : 1;
        });

        LocalDate firstDate = dates.getFirst();
        LocalDate lastDate = dates.getLast();
        String dateRange = firstDate.format(TR_DATE_FORMAT) + " - " + lastDate.format(TR_DATE_YEAR_FORMAT);
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
                        .existsByCreationDateAndPostTypeAndPostedToInstagramIsTrue(firstDate, "cover");
                if (checkShared) {
                    isPublished = true;
                    break;
                }
                System.out.println("[Scheduler] " + groupName + " paylaşımı bekleniyor... Deneme: "
                        + (attempt + 1) + "/" + maxAttempts);
            }

            if (isPublished) {
                String msg = "[Scheduler] " + groupName + " paylaşımı başarıyla tamamlandı.";
                System.out.println(msg);
                results.add(msg);
            } else {
                String msg = "[Scheduler] " + groupName + " paylaşımı zaman aşımına uğradı veya başarısız oldu.";
                System.err.println(msg);
                results.add(msg);
            }
        } catch (Exception ex) {
            String err = "[Scheduler] HATA! " + groupName + " paylaşımında kritik hata: " + ex.getMessage();
            System.err.println(err);
            ex.printStackTrace();
            results.add(err);
        }

        return results;
    }
} 