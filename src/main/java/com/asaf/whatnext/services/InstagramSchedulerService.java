package com.asaf.whatnext.services;

import com.asaf.whatnext.models.InstagramImage;
import com.asaf.whatnext.repository.InstagramImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ArrayList;

@Service
public class InstagramSchedulerService {

    @Autowired
    private InstagramService instagramService;

    @Autowired
    private InstagramImageRepository instagramImageRepository;
    private static final String DAILY_CAPTION_TEMPLATE = """
        📅 %s İstanbul Etkinlikleri 🎫
        
        🎭 Tiyatro | 🎶 Konser | 🎤 Stand-up  
        Bugün şehirde ne var? Kaçırma!
        
        🔗 Detaylar ve biletler profilde  
        #istanbuletkinlikleri #tiyatro #konser #standup #bugünnevar #whatnext #etkinlikrehberi #istanbuldaetkinlik
        """;



    @Scheduled(cron = "0 0 1 * * ?")
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

    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void shareDailyStoryToInstagram() {
        System.out.println("[Scheduler] Günlük story paylaşma görevi başlatıldı.");
        try {
            LocalDate today = LocalDate.now();
            if (instagramImageRepository.existsByCreationDateAndPostTypeAndPostedToInstagramIsTrue(today, "story")) {
                System.out.println("[Scheduler] Bugün için zaten bir story paylaşılmış. İşlem atlanıyor.");
                return;
            }

            Optional<InstagramImage> storyToShare = instagramImageRepository.findByPostTypeAndEventTypeAndCreationDate("story", "smart_story", today);
            if (storyToShare.isPresent() && !storyToShare.get().isPostedToInstagram()) {
                instagramService.shareStory(storyToShare.get());
                System.out.println("[Scheduler] Instagram story paylaşım isteği gönderildi.");
            } else {
                System.out.println("[Scheduler] Paylaşılacak story bulunamadı. Etkinlik olmadığı için story görseli üretilmemiş olabilir.");
            }
        } catch (Exception e) {
            System.err.println("[Scheduler] Instagram story paylaşımı sırasında hata: " + e.getMessage());
        }
    }

    @Scheduled(cron = "0 15 15 ? * SUN")
    @Transactional
    public void createWeeklyPosts() {
        System.out.println("[Scheduler] Haftalık post görselleri oluşturuluyor...");
        try {
            LocalDate today = LocalDate.now();
            // Önümüzdeki haftanın pazartesi gününü bul
            LocalDate nextMonday = today.plusDays((8 - today.getDayOfWeek().getValue()) % 7);
            // Pazartesi'den pazar'a kadar 7 gün için döngü
            for (int i = 0; i < 7; i++) {
                LocalDate targetDate = nextMonday.plusDays(i);
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
            }
            System.out.println("[Scheduler] Haftalık post görselleri başarıyla oluşturuldu.");
        } catch (Exception e) {
            System.err.println("[Scheduler] Haftalık postlar oluşturulamadı: " + e.getMessage());
        }
    }

    // Her pazar 14:00'te haftanın postlarını paylaş
    @Scheduled(cron = "0 30 15 ? * SUN")
    @Transactional
    public List<String> shareWeeklyPostsToInstagram() throws InterruptedException {
        List<String> results = new ArrayList<>();
        System.out.println("[Scheduler] Haftalık postları Instagram'a gönderme işlemi başlatılıyor...");
        LocalDate today = LocalDate.now();
        LocalDate nextMonday = today.plusDays((8 - today.getDayOfWeek().getValue()) % 7);
        // Önümüzdeki haftanın pazartesi gününü bul
        // LocalDate nextMonday = today.plusDays((8 - today.getDayOfWeek().getValue()) % 7);
        for (int i = 0; i < 7; i++) {
            LocalDate targetDate = nextMonday.plusDays(i);
            try {
                System.out.println("[Scheduler] İşlenen gün: " + targetDate);
                boolean coverShared = instagramImageRepository.existsByCreationDateAndPostTypeAndPostedToInstagramIsTrue(targetDate, "cover");
                if (coverShared) {
                    String msg = "[Scheduler] " + targetDate + " için zaten bir cover paylaşılmış. İşlem atlanıyor.";
                    System.out.println(msg);
                    results.add(msg);
                    continue;
                }
                List<InstagramImage> imagesToShare = instagramImageRepository.findByCreationDateAndPostedToInstagramFalse(targetDate);
                System.out.println("[Scheduler] " + targetDate + " için paylaşılacak toplam görsel: " + imagesToShare.size());
                List<InstagramImage> posts = new ArrayList<>(
                    imagesToShare.stream()
                        .filter(img -> "post".equals(img.getPostType()) || "cover".equals(img.getPostType()))
                        .toList()
                );
                System.out.println("[Scheduler] " + targetDate + " için paylaşılacak post/cover sayısı: " + posts.size());
                if (!posts.isEmpty()) {
                    posts.sort((img1, img2) -> "cover".equals(img1.getEventType()) ? -1 : 1);
                    String date = targetDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("tr")));
                    String caption = String.format(DAILY_CAPTION_TEMPLATE, date);
                    System.out.println("[Scheduler] " + targetDate + " için caption: " + caption);
                    System.out.println("[Scheduler] " + targetDate + " için paylaşılacak post id'leri: " + posts.stream().map(img -> img.getId() + ":" + img.getPostType() + ":" + img.getEventType()).toList());
                    instagramService.sharePost(posts, caption);
                    String msg = "[Scheduler] " + targetDate + " için haftalık postlar paylaşım isteği gönderildi.";
                    System.out.println(msg);
                    results.add(msg);
                } else {
                    String msg = "[Scheduler] " + targetDate + " için paylaşılacak haftalık post bulunamadı. Etkinlik olmadığı için görsel üretilmemiş olabilir.";
                    System.out.println(msg);
                    results.add(msg);
                }
            } catch (Exception ex) {
                String err = "[Scheduler] HATA! " + targetDate + " için paylaşım sırasında exception: " + ex.getMessage();
                System.err.println(err);
                ex.printStackTrace();
                results.add(err);
            }
            Thread.sleep(15000);
        }
        return results;
    }
} 