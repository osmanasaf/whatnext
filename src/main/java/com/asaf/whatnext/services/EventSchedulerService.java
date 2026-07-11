package com.asaf.whatnext.services;

import com.asaf.whatnext.scraper.EventScraperService;
import com.asaf.whatnext.utils.TrClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class EventSchedulerService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EventSchedulerService.class);
    
    @Autowired
    private EventScraperService eventScraperService;
    
    private final List<String> ACTIVE_CITIES = new ArrayList<>(Arrays.asList("istanbul"));
    private final List<String> FUTURE_CITIES = Arrays.asList("ankara", "izmir", "antalya");
    
    @Scheduled(cron = "0 0 2 L * ?", zone = "Europe/Istanbul")
    @Transactional
    public void scrapeMonthlyEvents() {
        LOGGER.info("=== AYLIK ETKİNLİK SCRAPING İŞLEMİ BAŞLATILDI ===");

        LocalDate today = TrClock.today();
        LocalDate nextMonth = today.plusMonths(1);
        LocalDate nextMonthStart = nextMonth.withDayOfMonth(1);
        LocalDate nextMonthEnd = nextMonth.withDayOfMonth(nextMonth.lengthOfMonth());
        
        LOGGER.info("Sonraki ay için etkinlikler çekiliyor: {} - {}", 
            nextMonthStart.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("tr"))),
            nextMonthEnd.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("tr"))));
        
        for (String city : new ArrayList<>(ACTIVE_CITIES)) {
            try {
                LOGGER.info("{} şehri için aylık etkinlikler çekiliyor...", city.toUpperCase());
                eventScraperService.scrapeAllEvents(city, nextMonthStart, nextMonthEnd);
                LOGGER.info("{} şehri için aylık etkinlikler başarıyla çekildi.", city.toUpperCase());
            } catch (Exception e) {
                LOGGER.error("{} şehri için aylık etkinlik çekme hatası: {}", city.toUpperCase(), e.getMessage());
            }
        }
        
        LOGGER.info("=== AYLIK ETKİNLİK SCRAPING İŞLEMİ TAMAMLANDI ===");
    }
    
    @Scheduled(cron = "0 0 3 ? * SUN", zone = "Europe/Istanbul")
    @Transactional
    public void scrapeWeeklyEvents() {
        LOGGER.info("=== HAFTALIK ETKİNLİK SCRAPING İŞLEMİ BAŞLATILDI ===");

        LocalDate today = TrClock.today();
        LocalDate nextWeekStart = today.plusDays(1);
        LocalDate nextWeekEnd = today.plusDays(14);
        
        LOGGER.info("Sonraki 14 gün için etkinlikler çekiliyor: {} - {}", 
            nextWeekStart.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("tr"))),
            nextWeekEnd.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("tr"))));
        
        for (String city : new ArrayList<>(ACTIVE_CITIES)) {
            try {
                LOGGER.info("{} şehri için haftalık etkinlikler çekiliyor...", city.toUpperCase());
                eventScraperService.scrapeAllEvents(city, nextWeekStart, nextWeekEnd);
                LOGGER.info("{} şehri için haftalık etkinlikler başarıyla çekildi.", city.toUpperCase());
            } catch (Exception e) {
                LOGGER.error("{} şehri için haftalık etkinlik çekme hatası: {}", city.toUpperCase(), e.getMessage());
            }
        }
        
        LOGGER.info("=== HAFTALIK ETKİNLİK SCRAPING İŞLEMİ TAMAMLANDI ===");
    }
    
    public synchronized void addCityToActiveList(String city) {
        if (FUTURE_CITIES.contains(city.toLowerCase())) {
            if (!ACTIVE_CITIES.contains(city.toLowerCase())) {
                LOGGER.info("{} şehri aktif şehirler listesine ekleniyor...", city.toUpperCase());
                ACTIVE_CITIES.add(city.toLowerCase());
                LOGGER.info("{} şehri başarıyla eklendi. Bir sonraki scraping işleminde dahil edilecek.", city.toUpperCase());
            } else {
                LOGGER.warn("{} şehri zaten aktif şehirler listesinde.", city.toUpperCase());
            }
        } else {
            LOGGER.error("{} şehri desteklenen şehirler listesinde bulunamadı.", city.toUpperCase());
        }
    }
    
    public synchronized void removeCityFromActiveList(String city) {
        if (ACTIVE_CITIES.contains(city.toLowerCase())) {
            if (ACTIVE_CITIES.size() > 1) {
                LOGGER.info("{} şehri aktif şehirler listesinden çıkarılıyor...", city.toUpperCase());
                ACTIVE_CITIES.remove(city.toLowerCase());
                LOGGER.info("{} şehri başarıyla çıkarıldı.", city.toUpperCase());
            } else {
                LOGGER.warn("En az bir aktif şehir olmalıdır. {} şehri çıkarılamadı.", city.toUpperCase());
            }
        } else {
            LOGGER.warn("{} şehri aktif şehirler listesinde bulunamadı.", city.toUpperCase());
        }
    }
    
    public List<String> getActiveCities() {
        return new ArrayList<>(ACTIVE_CITIES);
    }
    
    public List<String> getFutureCities() {
        return new ArrayList<>(FUTURE_CITIES);
    }
    
    public void scrapeEventsForCity(String city, LocalDate startDate, LocalDate endDate) {
        LOGGER.info("{} şehri için manuel scraping başlatılıyor: {} - {}", 
            city.toUpperCase(),
            startDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("tr"))),
            endDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("tr"))));
        
        try {
            eventScraperService.scrapeAllEvents(city, startDate, endDate);
            LOGGER.info("{} şehri için manuel scraping başarıyla tamamlandı.", city.toUpperCase());
        } catch (Exception e) {
            LOGGER.error("{} şehri için manuel scraping hatası: {}", city.toUpperCase(), e.getMessage());
            throw e;
        }
    }
} 