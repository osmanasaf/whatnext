package com.asaf.whatnext.service;

import com.asaf.whatnext.models.InstagramImage;
import com.asaf.whatnext.repository.InstagramImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class InstagramSchedulerService {

    @Autowired
    private InstagramService instagramService;

    @Autowired
    private InstagramImageRepository instagramImageRepository;

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
            instagramService.createImage("cover", "cover", null, today);
            instagramService.createImage("concert", "post", "istanbul", today);
            instagramService.createImage("theater", "post", "istanbul", today);
            instagramService.createImage("standup", "post", "istanbul", today);
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

            boolean coverShared = instagramImageRepository.existsByCreationDateAndPostTypeAndPostedToInstagramIsTrue(today, "cover");
            if (coverShared) {
                System.out.println("[Scheduler] Bugün için zaten bir cover paylaşılmış. İşlem atlanıyor.");
                return;
            }

            List<InstagramImage> imagesToShare = instagramImageRepository.findByCreationDateAndPostedToInstagramFalse(today);
            List<InstagramImage> posts = imagesToShare.stream()
                .filter(img -> "post".equals(img.getPostType()) || "cover".equals(img.getPostType()))
                .toList();
            
            if (!posts.isEmpty()) {
                posts.sort((img1, img2) -> "cover".equals(img1.getEventType()) ? -1 : 1);
                String caption = today.format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("tr"))) + " Etkinlikleri";
                instagramService.sharePost(posts, caption);
                System.out.println("[Scheduler] Haftalık postlar için paylaşım isteği gönderildi.");
            } else {
                System.out.println("[Scheduler] Paylaşılacak haftalık post bulunamadı veya zaten paylaşılmış.");
            }
        } catch (Exception e) {
            System.err.println("[Scheduler] Haftalık postlar Instagram'a gönderilemedi: " + e.getMessage());
        }
    }
} 