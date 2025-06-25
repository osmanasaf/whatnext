package com.asaf.whatnext.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service
public class InstagramSchedulerService {

    @Autowired
    private InstagramPostService instagramPostService;

    // Her gün saat 10:00'da çalışır
    @Scheduled(cron = "0 0 10 * * ?")
    public void createDailyPosts() {
        System.out.println("Starting daily post generation...");
        try {
            LocalDate today = LocalDate.now();
            instagramPostService.createPost("concert", "post", "istanbul", today);
            instagramPostService.createPost("theater", "post", "istanbul", today);
            instagramPostService.createPost("standup", "post", "istanbul", today);
        } catch (Exception e) {
            System.err.println("Error during daily post generation: " + e.getMessage());
        }
    }

    // Her gün saat 11:00'da çalışır
    @Scheduled(cron = "0 0 11 * * ?")
    public void createDailyStories() {
        System.out.println("Starting daily story generation...");
        try {
            LocalDate today = LocalDate.now();
            instagramPostService.createSmartStory(today);
        } catch (Exception e) {
            System.err.println("Error during daily story generation: " + e.getMessage());
        }
    }

    // Her Pazar saat 12:00'de haftanın gönderilerini oluşturur
    @Scheduled(cron = "0 0 12 * * SUN")
    public void createWeeklyDump() {
        System.out.println("Starting weekly Instagram dump for the next 7 days...");
        try {
            LocalDate startDate = LocalDate.now();
            for (int i = 0; i < 7; i++) {
                LocalDate targetDate = startDate.plusDays(i);
                System.out.println("Generating dump for date: " + targetDate);
                instagramPostService.createCover(targetDate);
                instagramPostService.createPost("concert", "post", "istanbul", targetDate);
                instagramPostService.createPost("theater", "post", "istanbul", targetDate);
                instagramPostService.createPost("standup", "post", "istanbul", targetDate);
            }
        } catch (Exception e) {
            System.err.println("Error during weekly dump generation: " + e.getMessage());
        }
    }
} 