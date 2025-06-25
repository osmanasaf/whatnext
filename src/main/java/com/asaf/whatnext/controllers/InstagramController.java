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

    @PostMapping("/story/{eventType}")
    public ResponseEntity<String> createInstagramStory(@PathVariable String eventType, @RequestParam(required = false) String date) {
        try {
            LocalDate targetDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : LocalDate.now();
            String imagePath = instagramPostService.createPost(eventType, "story", "istanbul", targetDate);
            if (imagePath != null) {
                return ResponseEntity.ok("Story created successfully at: " + imagePath);
            } else {
                return ResponseEntity.status(500).body("Failed to create story.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error creating story: " + e.getMessage());
        }
    }

    @PostMapping("/generate-all")
    public ResponseEntity<String> generateAllPostsAndStories(@RequestParam(required = false) String date) {
        try {
            LocalDate targetDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : LocalDate.now();
            instagramPostService.createPost("concert", "post", "istanbul", targetDate);
            instagramPostService.createPost("theater", "post", "istanbul", targetDate);
            instagramPostService.createPost("standup", "post", "istanbul", targetDate);
            instagramPostService.createPost("concert", "story", "istanbul", targetDate);
            instagramPostService.createPost("theater", "story", "istanbul", targetDate);
            instagramPostService.createPost("standup", "story", "istanbul", targetDate);
            return ResponseEntity.ok("All posts and stories created successfully for date: " + targetDate);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
} 