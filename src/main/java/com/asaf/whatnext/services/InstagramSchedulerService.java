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



    @Scheduled(cron = "0 0 1 * * ?", zone = "Europe/Istanbul")
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

    @Scheduled(cron = "0 0 16 ? * SUN", zone = "Europe/Istanbul")
    public void createWeeklyPosts() {
        System.out.println("[Scheduler] Haftalık post görselleri oluşturuluyor...");
        LocalDate today = LocalDate.now();
        // Önümüzdeki haftanın pazartesi gününü bul
        LocalDate nextMonday = today.plusDays((8 - today.getDayOfWeek().getValue()) % 7);
        
        // Pazartesi'den pazar'a kadar 7 gün için döngü
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

    // Her pazar 16:30'da haftanın postlarını paylaş
    @Scheduled(cron = "0 30 16 ? * SUN", zone = "Europe/Istanbul")
    @Transactional
    public List<String> shareWeeklyPostsToInstagram() throws InterruptedException {
        List<String> results = new ArrayList<>();
        System.out.println("[Scheduler] Haftalık postları Instagram'a gönderme işlemi başlatılıyor...");
        LocalDate today = LocalDate.now();
        LocalDate nextMonday = today.plusDays((8 - today.getDayOfWeek().getValue()) % 7);

        for (int i = 0; i < 7; i++) {
            LocalDate targetDate = nextMonday.plusDays(i);
            try {
                System.out.println("[Scheduler] İşlenen gün: " + targetDate);
                
                // 1. Daha önce paylaşılmış mı kontrolü
                boolean coverShared = instagramImageRepository.existsByCreationDateAndPostTypeAndPostedToInstagramIsTrue(targetDate, "cover");
                if (coverShared) {
                    String msg = "[Scheduler] " + targetDate + " için zaten bir cover paylaşılmış. Diğer güne geçiliyor.";
                    System.out.println(msg);
                    results.add(msg);
                    continue;
                }

                // 2. Paylaşılacak görselleri çek ve filtrele
                List<InstagramImage> imagesToShare = instagramImageRepository.findByCreationDateAndPostedToInstagramFalse(targetDate);
                List<InstagramImage> posts = new ArrayList<>();
                
                // Duplicate önleme: Her eventType'dan en son oluşturulanı al
                java.util.Map<String, InstagramImage> uniqueImages = new java.util.HashMap<>();
                for (InstagramImage img : imagesToShare) {
                    if ("post".equals(img.getPostType()) || "cover".equals(img.getPostType())) {
                        // Aynı eventType varsa (örn: 2 tane cover), en son kaydedileni (ID'si büyük olanı) tut
                        if (!uniqueImages.containsKey(img.getEventType()) || img.getId() > uniqueImages.get(img.getEventType()).getId()) {
                             uniqueImages.put(img.getEventType(), img);
                        }
                    }
                }
                posts.addAll(uniqueImages.values());

                if (!posts.isEmpty()) {
                    // Sıralama: Cover en başta olsun
                    posts.sort((img1, img2) -> "cover".equals(img1.getEventType()) ? -1 : 1);
                    
                    String date = targetDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("tr")));
                    String caption = String.format(DAILY_CAPTION_TEMPLATE, date);
                    
                    System.out.println("[Scheduler] " + targetDate + " için paylaşım başlatılıyor. Görsel ID'leri: " + posts.stream().map(InstagramImage::getId).toList());
                    
                    // Paylaşım isteğini gönder
                    instagramService.sharePost(posts, caption);
                    
                    // 3. Paylaşımın sonucunu bekle (Polling)
                    // Maksimum 5 dakika (30 x 10sn) bekle
                    boolean isPublished = false;
                    for (int attempt = 0; attempt < 30; attempt++) {
                        Thread.sleep(10000); // 10 saniye bekle
                        boolean checkShared = instagramImageRepository.existsByCreationDateAndPostTypeAndPostedToInstagramIsTrue(targetDate, "cover");
                        if (checkShared) {
                            isPublished = true;
                            break;
                        }
                        System.out.println("[Scheduler] " + targetDate + " paylaşımı bekleniyor... Deneme: " + (attempt + 1));
                    }

                    if (isPublished) {
                        String msg = "[Scheduler] ✅ " + targetDate + " paylaşımı başarıyla tamamlandı.";
                        System.out.println(msg);
                        results.add(msg);
                    } else {
                        String msg = "[Scheduler] ❌ " + targetDate + " paylaşımı zaman aşımına uğradı veya başarısız oldu. Sonraki günler İPTAL edildi.";
                        System.err.println(msg);
                        results.add(msg);
                        break; // Döngüyü kır, diğer günlere geçme!
                    }
                } else {
                    String msg = "[Scheduler] " + targetDate + " için paylaşılacak görsel bulunamadı.";
                    System.out.println(msg);
                    results.add(msg);
                }
            } catch (Exception ex) {
                String err = "[Scheduler] HATA! " + targetDate + " işleminde kritik hata: " + ex.getMessage();
                System.err.println(err);
                ex.printStackTrace();
                results.add(err);
                break; // Hata durumunda akışı durdur
            }
        }
        return results;
    }
} 