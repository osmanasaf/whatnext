package com.asaf.whatnext.services;

import com.asaf.whatnext.models.InstagramImage;
import com.asaf.whatnext.repository.InstagramImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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
    public void createDailyStory() {
        System.out.println("[Scheduler] Günlük story görseli oluşturma ve kaydetme görevi başlatıldı.");
        try {
            LocalDate today = LocalDate.now();
            boolean exists = instagramImageRepository.findByPostTypeAndEventTypeAndCreationDate("story", "smart_story", today).isPresent();
            if (!exists) {
                instagramService.createImage( "smart_story", "story", null, today);
                System.out.println("[Scheduler] Günlük story görseli başarıyla oluşturuldu ve veritabanına kaydedildi.");
            } else {
                System.out.println("[Scheduler] Bugünün story görseli zaten mevcut, oluşturma işlemi atlandı.");
            }
        } catch (Exception e) {
            System.err.println("[Scheduler] Story oluşturma hatası: " + e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 12 * * ?")
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
                System.out.println("[Scheduler] Paylaşılacak story bulunamadı veya zaten paylaşılmış.");
            }
        } catch (Exception e) {
            System.err.println("[Scheduler] Instagram story paylaşımı sırasında hata: " + e.getMessage());
        }
    }

    @Scheduled(cron = "0 15 1 ? * SUN")
    public void createWeeklyPosts() {
        System.out.println("[Scheduler] Haftalık post görselleri oluşturuluyor...");
        try {
            LocalDate today = LocalDate.now();
            // Önümüzdeki haftanın pazartesi gününü bul
            LocalDate nextMonday = today.plusDays((8 - today.getDayOfWeek().getValue()) % 7);
            // Pazartesi'den pazar'a kadar 7 gün için döngü
            for (int i = 0; i < 7; i++) {
                LocalDate targetDate = nextMonday.plusDays(i);
                instagramService.createImage("cover", "cover", null, targetDate);
                instagramService.createImage("concert", "post", "istanbul", targetDate);
                instagramService.createImage("theater", "post", "istanbul", targetDate);
                instagramService.createImage("standup", "post", "istanbul", targetDate);
            }
            System.out.println("[Scheduler] Haftalık post görselleri başarıyla oluşturuldu.");
        } catch (Exception e) {
            System.err.println("[Scheduler] Haftalık postlar oluşturulamadı: " + e.getMessage());
        }
    }

    // Her pazar 14:00'te haftanın postlarını paylaş
    @Scheduled(cron = "0 0 14 ? * SUN")
    public void shareWeeklyPostsToInstagram() {
        System.out.println("[Scheduler] Haftalık postları Instagram'a gönderme işlemi başlatılıyor...");
        try {
            LocalDate today = LocalDate.now();
            // Önümüzdeki haftanın pazartesi gününü bul
            LocalDate nextMonday = today.plusDays((8 - today.getDayOfWeek().getValue()) % 7);
            for (int i = 0; i < 7; i++) {
                LocalDate targetDate = nextMonday.plusDays(i);
                boolean coverShared = instagramImageRepository.existsByCreationDateAndPostTypeAndPostedToInstagramIsTrue(targetDate, "cover");
                if (coverShared) {
                    System.out.println("[Scheduler] " + targetDate + " için zaten bir cover paylaşılmış. İşlem atlanıyor.");
                    continue;
                }
                List<InstagramImage> imagesToShare = instagramImageRepository.findByCreationDateAndPostedToInstagramFalse(targetDate);
                List<InstagramImage> posts = imagesToShare.stream()
                    .filter(img -> "post".equals(img.getPostType()) || "cover".equals(img.getPostType()))
                    .toList();
                if (!posts.isEmpty()) {
                    posts.sort((img1, img2) -> "cover".equals(img1.getEventType()) ? -1 : 1);
                    String date = targetDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("tr")));
                    String caption = String.format(DAILY_CAPTION_TEMPLATE, date);
                    instagramService.sharePost(posts, caption);
                    System.out.println("[Scheduler] " + targetDate + " için haftalık postlar paylaşım isteği gönderildi.");
                } else {
                    System.out.println("[Scheduler] " + targetDate + " için paylaşılacak haftalık post bulunamadı veya zaten paylaşılmış.");
                }
            }
        } catch (Exception e) {
            System.err.println("[Scheduler] Haftalık postlar Instagram'a gönderilemedi: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 