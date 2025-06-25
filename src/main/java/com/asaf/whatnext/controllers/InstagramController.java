package com.asaf.whatnext.controllers;

import com.asaf.whatnext.service.InstagramPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/instagram")
public class InstagramController {

    @Autowired
    private InstagramPostService instagramPostService;

    @PostMapping("/post/{eventType}")
    public ResponseEntity<String> createInstagramPost(@PathVariable String eventType, @RequestParam(required = false) String date) {
        try {
            LocalDate targetDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : LocalDate.now();
            String imagePath = instagramPostService.createPost(eventType, "post", "istanbul", targetDate);
            if (imagePath != null) {
                return ResponseEntity.ok("Post created successfully at: " + imagePath);
            } else {
                return ResponseEntity.status(500).body("Failed to create post.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error creating post: " + e.getMessage());
        }
    }

    @PostMapping("/story")
    public ResponseEntity<String> createSmartStory(@RequestParam(required = false) String date) {
        try {
            LocalDate targetDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : LocalDate.now();
            String imagePath = instagramPostService.createSmartStory(targetDate);
            if (imagePath != null) {
                return ResponseEntity.ok("Smart story created successfully at: " + imagePath);
            } else {
                return ResponseEntity.status(500).body("Failed to create smart story.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error creating smart story: " + e.getMessage());
        }
    }

    @PostMapping("/cover")
    public ResponseEntity<String> createCover(@RequestParam(required = false) String date) {
        try {
            LocalDate targetDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : LocalDate.now();
            String imagePath = instagramPostService.createCover(targetDate);
            if (imagePath != null) {
                return ResponseEntity.ok("Cover created successfully at: " + imagePath);
            } else {
                return ResponseEntity.status(500).body("Failed to create cover.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error creating cover: " + e.getMessage());
        }
    }

    @PostMapping("/generate-all")
    public ResponseEntity<String> generateAllPostsAndStories(@RequestParam(required = false) String date) {
        try {
            LocalDate targetDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : LocalDate.now();
            StringBuilder result = new StringBuilder();
            String coverPath = instagramPostService.createCover(targetDate);
            result.append("Cover: ").append(coverPath).append("\n");
            String concertPath = instagramPostService.createPost("concert", "post", "istanbul", targetDate);
            result.append("Concert: ").append(concertPath).append("\n");
            String theaterPath = instagramPostService.createPost("theater", "post", "istanbul", targetDate);
            result.append("Theater: ").append(theaterPath).append("\n");
            String standupPath = instagramPostService.createPost("standup", "post", "istanbul", targetDate);
            result.append("Standup: ").append(standupPath).append("\n");
            String storyPath = instagramPostService.createSmartStory(targetDate);
            result.append("Story: ").append(storyPath).append("\n");
            return ResponseEntity.ok(result.toString());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
} 