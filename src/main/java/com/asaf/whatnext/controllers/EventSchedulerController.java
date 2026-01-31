package com.asaf.whatnext.controllers;

import com.asaf.whatnext.services.EventSchedulerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/scheduler")
public class EventSchedulerController {

    @Autowired
    private EventSchedulerService eventSchedulerService;

    @GetMapping("/cities/active")
    public ResponseEntity<List<String>> getActiveCities() {
        return ResponseEntity.ok(eventSchedulerService.getActiveCities());
    }

    @GetMapping("/cities/future")
    public ResponseEntity<List<String>> getFutureCities() {
        return ResponseEntity.ok(eventSchedulerService.getFutureCities());
    }

    @PostMapping("/cities/add")
    public ResponseEntity<String> addCityToActiveList(@RequestParam String city) {
        try {
            eventSchedulerService.addCityToActiveList(city);
            return ResponseEntity.ok(city.toUpperCase() + " şehri aktif listeye eklendi.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Hata: " + e.getMessage());
        }
    }

    @PostMapping("/cities/remove")
    public ResponseEntity<String> removeCityFromActiveList(@RequestParam String city) {
        try {
            eventSchedulerService.removeCityFromActiveList(city);
            return ResponseEntity.ok(city.toUpperCase() + " şehri aktif listeden çıkarıldı.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Hata: " + e.getMessage());
        }
    }

    @PostMapping("/monthly/run")
    public ResponseEntity<String> runMonthlyScraping() {
        try {
            eventSchedulerService.scrapeMonthlyEvents();
            return ResponseEntity.ok("Aylık scraping işlemi başarıyla tamamlandı.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Aylık scraping hatası: " + e.getMessage());
        }
    }

    @PostMapping("/weekly/run")
    public ResponseEntity<String> runWeeklyScraping() {
        try {
            eventSchedulerService.scrapeWeeklyEvents();
            return ResponseEntity.ok("Haftalık scraping işlemi başarıyla tamamlandı.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Haftalık scraping hatası: " + e.getMessage());
        }
    }

    @PostMapping("/city/run")
    public ResponseEntity<String> runScrapingForCity(
            @RequestParam String city,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            
            eventSchedulerService.scrapeEventsForCity(city, start, end);
            return ResponseEntity.ok(city.toUpperCase() + " şehri için scraping işlemi başarıyla tamamlandı.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Scraping hatası: " + e.getMessage());
        }
    }

    @GetMapping("/status")
    public ResponseEntity<String> getSchedulerStatus() {
        List<String> activeCities = eventSchedulerService.getActiveCities();
        List<String> futureCities = eventSchedulerService.getFutureCities();
        
        String status = String.format("""
            🕐 Event Scheduler Durumu
            
            ✅ Aktif Şehirler: %s
            🔄 Gelecekte Eklenecek Şehirler: %s
            
            📅 Aylık Scraping: Her ayın 30'unda saat 02:00
            📅 Haftalık Scraping: Her pazar saat 03:00
            
            🎯 Şu an sadece İstanbul için scraping yapılıyor.
            """, 
            String.join(", ", activeCities),
            String.join(", ", futureCities));
        
        return ResponseEntity.ok(status);
    }
} 