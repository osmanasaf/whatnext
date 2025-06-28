package com.asaf.whatnext.controllers;

import com.asaf.whatnext.models.InstagramImage;
import com.asaf.whatnext.repository.InstagramImageRepository;
import com.asaf.whatnext.services.InstagramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    private static final String DAILY_CAPTION_TEMPLATE = """
        📅 %s İstanbul Etkinlikleri 🎫
        
        🎭 Tiyatro | 🎶 Konser | 🎤 Stand-up  
        Bugün şehirde ne var? Kaçırma!
        
        🔗 Detaylar ve biletler profilde  
        #istanbuletkinlikleri #tiyatro #konser #standup #bugünnevar #whatnext #etkinlikrehberi #istanbuldaetkinlik
        """;

    @PostMapping("/create/{eventType}")
    public ResponseEntity<?> createInstagramPost(@PathVariable String eventType, @RequestParam(required = false) String date) {
        try {
            LocalDate targetDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : LocalDate.now();
            InstagramImage image = instagramService.createImage(eventType, "post", "istanbul", targetDate);
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
            results.add(instagramService.createImage("cover", "cover", null, targetDate));
            System.out.println("ℹ️ Konser postu oluşturuluyor...");
            results.add(instagramService.createImage("concert", "post", "istanbul", targetDate));
            System.out.println("ℹ️ Tiyatro postu oluşturuluyor...");
            results.add(instagramService.createImage("theater", "post", "istanbul", targetDate));
            System.out.println("ℹ️ Stand-up postu oluşturuluyor...");
            results.add(instagramService.createImage("standup", "post", "istanbul", targetDate));
             System.out.println("ℹ️ Akıllı story oluşturuluyor...");
            results.add(instagramService.createImage("smart_story", "story", null, targetDate));
            return ResponseEntity.ok(results);
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
                }
            }

            return ResponseEntity.ok(Map.of("success", true, "message", "Paylaşım istekleri kuyruğa eklendi."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error posting to Instagram: " + e.getMessage());
        }
    }
} 