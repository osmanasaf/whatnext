package com.asaf.whatnext.controllers;

import com.asaf.whatnext.models.InstagramImage;
import com.asaf.whatnext.repository.InstagramImageRepository;
import com.asaf.whatnext.services.InstagramService;
import com.asaf.whatnext.services.InstagramSchedulerService;
import com.asaf.whatnext.services.WeeklyVideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.asaf.whatnext.utils.TrClock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/instagram")
public class InstagramController {

    @Autowired
    private InstagramService instagramService;

    @Autowired
    private InstagramImageRepository instagramImageRepository;

    @Autowired
    private InstagramSchedulerService instagramSchedulerService;

    @Autowired
    private WeeklyVideoService weeklyVideoService;

    private static final String DAILY_CAPTION_TEMPLATE = """
        📅 %s İstanbul Etkinlikleri 🎫
        
        🎭 Tiyatro | 🎶 Konser | 🎤 Stand-up
        Bugün şehirde ne var? Kaçırma!
        
        🔗 Detaylar ve biletler profilde
        #istanbul #istanbuletkinlikleri #etkinlik #konser #tiyatro #sanat #istanbullife #istanbuldayasam #igersistanbul #istanbulnightlife #istanbulbosphorus #galatatower
        """;

    @PostMapping("/create/{eventType}")
    public ResponseEntity<?> createInstagramPost(@PathVariable String eventType, @RequestParam(required = false) String date) {
        try {
            LocalDate targetDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : LocalDate.now();
            InstagramImage image = instagramService.createImage(eventType, "post", "istanbul", targetDate);
            
            if (image == null) {
                return ResponseEntity.ok(Map.of(
                    "success", false, 
                    "message", targetDate + " tarihinde " + eventType + " türünde etkinlik bulunamadı. Görsel üretilmedi."
                ));
            }
            
            return ResponseEntity.ok(image);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error creating post: " + e.getMessage());
        }
    }

    @PostMapping("/create/story")
    public ResponseEntity<?> createSmartStory(@RequestParam(required = false) String date) {
        try {
            LocalDate targetDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : LocalDate.now();
            InstagramImage image = instagramService.createImage("smart_story", "story", null, targetDate);
            return ResponseEntity.ok(image);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error creating story: " + e.getMessage());
        }
    }

    @PostMapping("/create/cover")
    public ResponseEntity<?> createCover(@RequestParam(required = false) String date) {
        try {
            LocalDate targetDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : LocalDate.now();
            InstagramImage image = instagramService.createImage("cover", "cover", null, targetDate);
            return ResponseEntity.ok(image);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error creating cover: " + e.getMessage());
        }
    }
    
    @PostMapping("/generate-all")
    public ResponseEntity<?> generateAllPostsAndStories(@RequestParam(required = false) String date) {
        try {
            LocalDate targetDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : LocalDate.now();
            List<InstagramImage> results = new ArrayList<>();
            
            System.out.println("ℹ️ Günlük kapak görseli oluşturuluyor...");
            InstagramImage coverImage = instagramService.createImage("cover", "cover", null, targetDate);
            if (coverImage != null) {
                results.add(coverImage);
            }
            
            System.out.println("ℹ️ Konser postu oluşturuluyor...");
            InstagramImage concertImage = instagramService.createImage("concert", "post", "istanbul", targetDate);
            if (concertImage != null) {
                results.add(concertImage);
            }
            
            System.out.println("ℹ️ Tiyatro postu oluşturuluyor...");
            InstagramImage theaterImage = instagramService.createImage("theater", "post", "istanbul", targetDate);
            if (theaterImage != null) {
                results.add(theaterImage);
            }
            
            System.out.println("ℹ️ Stand-up postu oluşturuluyor...");
            InstagramImage standupImage = instagramService.createImage("standup", "post", "istanbul", targetDate);
            if (standupImage != null) {
                results.add(standupImage);
            }
            
            System.out.println("ℹ️ Akıllı story oluşturuluyor...");
            InstagramImage storyImage = instagramService.createImage("smart_story", "story", null, targetDate);
            if (storyImage != null) {
                results.add(storyImage);
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Görsel üretimi tamamlandı. " + results.size() + " görsel başarıyla oluşturuldu.",
                "images", results
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "Bir hata oluştu: " + e.getMessage()));
        }
    }

    @PostMapping("/share-for-date")
    public ResponseEntity<?> postToInstagram(@RequestParam(required = false) String date) {
        try {
            LocalDate targetDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : LocalDate.now();

            boolean storyShared = instagramImageRepository.existsByCreationDateAndPostTypeAndPostedToInstagramIsTrue(targetDate, "story");
            if(storyShared){
                System.out.println("ℹ️ Story for " + targetDate + " already shared. Skipping.");
            } else {
                 instagramImageRepository.findByCreationDateAndPostedToInstagramFalse(targetDate).stream()
                    .filter(img -> "story".equals(img.getPostType()))
                    .findFirst()
                    .ifPresent(instagramService::shareStory);
            }

            boolean coverShared = instagramImageRepository.existsByCreationDateAndPostTypeAndPostedToInstagramIsTrue(targetDate, "cover");
             if(coverShared){
                System.out.println("ℹ️ Cover/Post for " + targetDate + " already shared. Skipping.");
            } else {
                List<InstagramImage> posts = instagramImageRepository.findByCreationDateAndPostedToInstagramFalse(targetDate).stream()
                    .filter(img -> "post".equals(img.getPostType()) || "cover".equals(img.getPostType()))
                    .sorted(Comparator.comparing(img -> "cover".equals(img.getEventType()) ? 0 : 1))
                    .collect(Collectors.toList());
                
                if(!posts.isEmpty()){
                    String captionDate = targetDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("tr")));
                    String caption = String.format(DAILY_CAPTION_TEMPLATE, captionDate);
                     instagramService.sharePost(posts, caption);
                } else {
                    System.out.println("⚠️ " + targetDate + " için paylaşılacak görsel bulunamadı.");
                    return ResponseEntity.ok(Map.of("success", false, "message", targetDate + " için paylaşılacak görsel bulunamadı."));
                }
            }

            return ResponseEntity.ok(Map.of("success", true, "message", "Paylaşım istekleri kuyruğa eklendi."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error posting to Instagram: " + e.getMessage());
        }
    }

    @PostMapping("/scheduler/share-daily-story")
    public ResponseEntity<?> shareDailyStoryManually(@RequestParam(required = false) String date) {
        try {
            LocalDate targetDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : LocalDate.now();

            if (instagramImageRepository.existsByCreationDateAndPostTypeAndPostedToInstagramIsTrue(targetDate, "story")) {
                return ResponseEntity.ok(Map.of("success", false, "message", targetDate + " için zaten bir story paylaşılmış."));
            }

            InstagramImage story = instagramImageRepository
                .findByPostTypeAndEventTypeAndCreationDate("story", "smart_story", targetDate)
                .orElse(null);

            if (story == null) {
                story = instagramService.createImage("smart_story", "story", null, targetDate);
                if (story == null) {
                    return ResponseEntity.ok(Map.of("success", false, "message", targetDate + " için etkinlik bulunamadı, story görseli üretilemedi."));
                }
            }

            instagramService.shareStory(story);
            return ResponseEntity.ok(Map.of("success", true, "message", "Story paylaşım isteği kuyruğa eklendi.", "imageId", story.getId()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "Hata: " + e.getMessage()));
        }
    }

    /**
     * Haftalık görseller: parametresiz = Pazar cron ile aynı hafta (scheduledWeeklyMonday + 7 gün).
     * startDate + endDate (YYYY-MM-DD) = o aralıktaki her gün (ör. 2026-04-06 … 2026-04-12).
     */
    @PostMapping("/scheduler/create-weekly-posts")
    public ResponseEntity<?> createWeeklyPostsManually(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            if (startDate != null && !startDate.isBlank() && endDate != null && !endDate.isBlank()) {
                LocalDate start = LocalDate.parse(startDate);
                LocalDate end = LocalDate.parse(endDate);
                instagramSchedulerService.createWeeklyPostsForDateRange(start, end);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Görsel üretimi tamamlandı.",
                        "startDate", start.toString(),
                        "endDate", end.toString()));
            }
            instagramSchedulerService.createWeeklyPosts();
            return ResponseEntity.ok("Haftalık post görselleri manuel olarak oluşturuldu (bugünün haftası, Pazar cron mantığı).");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Hata: " + e.getMessage());
        }
    }

    /** Pazartesi 19:00 cron'unun elle hali. {@code monday} verilmezse bu haftanın Pazartesi'si. */
    @PostMapping("/scheduler/share-weekly-carousel")
    public ResponseEntity<?> shareWeeklyCarousel(@RequestParam(required = false) String monday) {
        LocalDate d = monday != null && !monday.isBlank()
                ? LocalDate.parse(monday) : TrClock.today().with(java.time.DayOfWeek.MONDAY);
        return ResponseEntity.ok(instagramSchedulerService.shareWeeklyCarousel(d));
    }

    /** Perşembe 19:00 cron'unun elle hali. {@code friday} verilmezse bu haftanın Cuma'sı. */
    @PostMapping("/scheduler/share-weekend-carousel")
    public ResponseEntity<?> shareWeekendCarousel(@RequestParam(required = false) String friday) {
        LocalDate d = friday != null && !friday.isBlank()
                ? LocalDate.parse(friday) : TrClock.today().with(java.time.DayOfWeek.FRIDAY);
        return ResponseEntity.ok(instagramSchedulerService.shareWeekendCarousel(d));
    }

    @PostMapping("/scheduler/generate-weekly-reel")
    public ResponseEntity<?> generateWeeklyReelManually() {
        try {
            weeklyVideoService.generateAndShareWeeklyReel();
            return ResponseEntity.ok("Haftalık Reel oluşturuldu ve paylaşım kuyruğa alındı.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Hata: " + e.getMessage());
        }
    }

    /**
     * Haftalık paylaşım: parametresiz = içinde bulunulan haftanın Pazartesi–Pazar 7 günü.
     * startDate + endDate = o aralık sırayla (ör. 2026-04-06 … 2026-04-12).
     */
    @PostMapping("/scheduler/share-weekly-posts")
    public ResponseEntity<?> shareWeeklyPostsManually(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            if (startDate != null && !startDate.isBlank() && endDate != null && !endDate.isBlank()) {
                LocalDate start = LocalDate.parse(startDate);
                LocalDate end = LocalDate.parse(endDate);
                List<String> results = instagramSchedulerService.shareFeedPostsForDateRange(start, end);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "startDate", start.toString(),
                        "endDate", end.toString(),
                        "log", results));
            }
            List<String> results = instagramSchedulerService.shareFullWeekFeedPosts();
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Hata: " + e.getMessage());
        }
    }
}