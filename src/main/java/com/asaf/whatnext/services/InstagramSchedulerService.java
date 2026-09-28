package com.asaf.whatnext.services;

import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.models.InstagramImage;
import com.asaf.whatnext.repository.InstagramImageRepository;
import com.asaf.whatnext.services.scoring.EventScoringStrategy;
import com.asaf.whatnext.utils.EventAudience;
import com.asaf.whatnext.utils.EventQuality;
import com.asaf.whatnext.utils.Hashtags;
import com.asaf.whatnext.utils.TrClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.ArrayList;

@Service
public class InstagramSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstagramSchedulerService.class);

    @Autowired
    private InstagramService instagramService;

    @Autowired
    private InstagramImageRepository instagramImageRepository;

    @Autowired
    private WeeklyVideoService weeklyVideoService;

    @Autowired
    private EventService eventService;

    @Autowired
    private EventScoringStrategy scoringStrategy;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private static final Locale TR = Locale.forLanguageTag("tr");
    private static final DateTimeFormatter DAY_MONTH_TR = DateTimeFormatter.ofPattern("d MMMM", TR);
    private static final DateTimeFormatter TR_DATE_YEAR_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("tr"));
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
    private static final int CAPTION_MAX_EVENTS = 8;

    /** Aynı gün içinde arka arkaya postlar arası (Instagram kuyruğu / limit) */
    private static final long DELAY_BETWEEN_DAILY_POSTS_MS = 90_000L;



    /*
     * Paylaşım takvimi (2026-09-28): Pazar 19:00 Reel (gelecek hafta — PopularEventService Perşembe'den
     * itibaren sonraki haftayı seçer), Pazartesi 19:00 haftalık karusel, Perşembe 19:00 hafta sonu karuseli,
     * her gün story. Eski düzen (7 günlük karusel, 3'ü art arda) erişimi bölüyordu.
     */

    @Scheduled(cron = "0 0 19 ? * SUN", zone = "Europe/Istanbul")
    public void generateAndShareWeeklyReel() {
        LOGGER.info("[Scheduler] Haftalık Reel video oluşturma ve paylaşım görevi başlatıldı.");
        try {
            weeklyVideoService.generateAndShareWeeklyReel();
            LOGGER.info("[Scheduler] Haftalık Reel görevi tamamlandı.");
        } catch (Exception e) {
            LOGGER.error("[Scheduler] Haftalık Reel hatası", e);
        }
    }

    @Scheduled(cron = "0 0 19 ? * MON", zone = "Europe/Istanbul")
    public void scheduledWeeklyCarousel() {
        LOGGER.info(shareWeeklyCarousel(TrClock.today().with(DayOfWeek.MONDAY)));
    }

    @Scheduled(cron = "0 0 19 ? * THU", zone = "Europe/Istanbul")
    public void scheduledWeekendCarousel() {
        LOGGER.info(shareWeekendCarousel(TrClock.today().with(DayOfWeek.FRIDAY)));
    }

    /** "Bu hafta İstanbul'da" kapağı + haftanın 7 gün kapağı (Pazar 16:00'da üretilenler). */
    public String shareWeeklyCarousel(LocalDate monday) {
        List<String[]> slides = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            slides.add(new String[]{monday.plusDays(i).toString(), "cover"});
        }
        return shareCarousel("week", monday, 7, slides, buildWeeklyCaption(monday));
    }

    /** "Bu hafta sonu ne yapsak?" kapağı + Cuma/Cumartesi/Pazar konser-tiyatro-stand-up görselleri (≤10). */
    public String shareWeekendCarousel(LocalDate friday) {
        List<String[]> slides = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            for (String type : List.of("concert", "theater", "standup")) {
                slides.add(new String[]{friday.plusDays(i).toString(), type});
            }
        }
        return shareCarousel("weekend", friday, 3, slides, buildWeekendCaption(friday));
    }

    /**
     * Açılış kapağını ({@code hookType}) şimdi üretir, ardına mevcut gün görsellerinin en yenisini dizer.
     * Gün görselleri daha önce başka bir karuselde paylaşılmış olsa da kullanılır. Kapak zaten paylaşıldıysa atlar.
     */
    private String shareCarousel(String hookType, LocalDate start, int days, List<String[]> slides, String caption) {
        String tag = "[Scheduler] " + hookType + " karuseli " + start;
        try {
            List<InstagramImage> existing = instagramImageRepository.findByCreationDateBetween(start, start.plusDays(days - 1));
            if (existing.stream().anyMatch(img -> hookType.equals(img.getEventType()) && img.isPostedToInstagram())) {
                return tag + " zaten paylaşılmış, atlanıyor.";
            }
            InstagramImage hook = instagramService.createImage(hookType, "cover", null, start);
            if (hook == null) {
                return tag + ": kapak üretilemedi, paylaşılmadı.";
            }
            List<InstagramImage> images = new ArrayList<>(List.of(hook));
            for (String[] slide : slides) {
                existing.stream()
                        .filter(img -> slide[0].equals(String.valueOf(img.getCreationDate())) && slide[1].equals(img.getEventType()))
                        .max(Comparator.comparing(InstagramImage::getId))
                        .ifPresent(images::add);
            }
            if (images.size() < 2) {
                return tag + ": gün görseli yok (Pazar 16:00 üretimi çalıştı mı?), paylaşılmadı.";
            }
            instagramService.sharePost(images.subList(0, Math.min(images.size(), 10)), caption);
            return tag + " kuyruğa alındı: " + images.size() + " görsel.";
        } catch (Exception e) {
            LOGGER.error(tag + " hatası", e);
            return tag + " HATA: " + e.getMessage();
        }
    }

    String buildWeeklyCaption(LocalDate monday) {
        StringBuilder sb = new StringBuilder("📅 Bu hafta İstanbul'da · ")
                .append(monday.format(DAY_MONTH_TR)).append(" – ").append(monday.plusDays(6).format(DAY_MONTH_TR))
                .append("\n\n");
        List<Event> all = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = monday.plusDays(i);
            List<Event> top = topEvents(d, 2);
            all.addAll(top);
            if (!top.isEmpty()) {
                sb.append(dayName(d)).append(": ")
                        .append(String.join(", ", top.stream().map(Event::getTitle).toList())).append('\n');
            }
        }
        sb.append("\n👉 Kaydır, her gün ayrı\n🔖 Kaydet, hafta boyunca lazım olacak!\n\n🔗 Detaylar ve biletler profilde\n");
        return sb.append(hashtagsFor(all)).toString();
    }

    String buildWeekendCaption(LocalDate friday) {
        StringBuilder sb = new StringBuilder("👀 Bu hafta sonu ne yapsak?\n\n");
        List<Event> all = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            LocalDate d = friday.plusDays(i);
            List<Event> top = topEvents(d, 3);
            all.addAll(top);
            if (!top.isEmpty()) {
                sb.append(dayName(d)).append('\n');
                top.forEach(e -> sb.append(eventLine(e)));
                sb.append('\n');
            }
        }
        sb.append("👯 Kiminle gidiyorsun? Etiketle!\n🔖 Kaydet, hafta sonu lazım olacak\n\n🔗 Detaylar ve biletler profilde\n");
        return sb.append(hashtagsFor(all)).toString();
    }

    private static String dayName(LocalDate d) {
        String n = d.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, TR);
        return n.substring(0, 1).toUpperCase(TR) + n.substring(1);
    }

    @Scheduled(cron = "0 50 11 * * ?", zone = "Europe/Istanbul")
    @Transactional
    public void createDailyStory() {
        LOGGER.info("[Scheduler] Günlük story görseli oluşturma ve kaydetme görevi başlatıldı.");
        try {
            LocalDate today = LocalDate.now();
            boolean exists = instagramImageRepository.findByPostTypeAndEventTypeAndCreationDate("story", "smart_story", today).isPresent();
            if (!exists) {
                InstagramImage storyImage = instagramService.createImage( "smart_story", "story", null, today);
                if (storyImage != null) {
                    LOGGER.info("[Scheduler] Günlük story görseli başarıyla oluşturuldu ve veritabanına kaydedildi.");
                } else {
                    LOGGER.info("[Scheduler] Bugün için hiç etkinlik bulunamadı. Story görseli oluşturulmadı.");
                }
            } else {
                LOGGER.info("[Scheduler] Bugünün story görseli zaten mevcut, oluşturma işlemi atlandı.");
            }
        } catch (Exception e) {
            LOGGER.error("[Scheduler] Story oluşturma hatası", e);
        }
    }

    @Scheduled(cron = "0 0 12 * * ?", zone = "Europe/Istanbul")
    @Transactional
    public void shareDailyStoryToInstagram() {
        LOGGER.info("[Scheduler] Günlük story paylaşma görevi başlatıldı.");
        try {
            LocalDate today = LocalDate.now();
            if (instagramImageRepository.existsByCreationDateAndPostTypeAndPostedToInstagramIsTrue(today, "story")) {
                LOGGER.info("[Scheduler] Bugün için zaten bir story paylaşılmış. İşlem atlanıyor.");
                return;
            }

            Optional<InstagramImage> storyToShare = instagramImageRepository
                .findByPostTypeAndEventTypeAndCreationDate("story", "smart_story", today);

            // Görsel henüz oluşturulmamışsa burada oluştur
            if (storyToShare.isEmpty()) {
                LOGGER.info("[Scheduler] Bugün için story görseli bulunamadı, şimdi oluşturuluyor...");
                InstagramImage created = instagramService.createImage("smart_story", "story", null, today);
                if (created != null) {
                    storyToShare = Optional.of(created);
                    LOGGER.info("[Scheduler] Story görseli oluşturuldu, paylaşılacak.");
                } else {
                    LOGGER.info("[Scheduler] Bugün için etkinlik bulunamadı, story görseli üretilemedi.");
                    return;
                }
            }

            if (!storyToShare.get().isPostedToInstagram()) {
                instagramService.shareStory(storyToShare.get());
                LOGGER.info("[Scheduler] Instagram story paylaşım isteği gönderildi.");
            }
        } catch (Exception e) {
            LOGGER.error("[Scheduler] Instagram story paylaşımı sırasında hata", e);
        }
    }

    /**
     * Pazar cron ile aynı mantık: o haftanın Pazartesi + 7 gün (bugüne göre hesaplanan hafta).
     */
    @Scheduled(cron = "0 0 16 ? * SUN", zone = "Europe/Istanbul")
    public void createWeeklyPosts() {
        LocalDate monday = scheduledWeeklyMonday(LocalDate.now());
        createWeeklyPostsForDateRange(monday, monday.plusDays(6));
    }

    /**
     * Pazar job'undaki hafta Pazartesi (mevcut formül).
     */
    static LocalDate scheduledWeeklyMonday(LocalDate today) {
        return today.plusDays((8 - today.getDayOfWeek().getValue()) % 7);
    }

    /**
     * Verilen aralıkta (her iki uç dahil) her gün için cover + konser + tiyatro + stand-up görselleri üretir.
     */
    public void createWeeklyPostsForDateRange(LocalDate startInclusive, LocalDate endInclusive) {
        if (endInclusive.isBefore(startInclusive)) {
            throw new IllegalArgumentException("endDate startDate'den önce olamaz");
        }
        LOGGER.info("[Scheduler] Post görselleri oluşturuluyor: " + startInclusive + " … " + endInclusive);
        for (LocalDate targetDate = startInclusive; !targetDate.isAfter(endInclusive); targetDate = targetDate.plusDays(1)) {
            createDailyFeedImages(targetDate);
        }
        LOGGER.info("[Scheduler] Görsel oluşturma aralığı tamamlandı.");
    }

    private void createDailyFeedImages(LocalDate targetDate) {
        try {
            LOGGER.info("[Scheduler] " + targetDate + " için görseller oluşturuluyor...");

            InstagramImage coverImage = instagramService.createImage("cover", "cover", null, targetDate);
            if (coverImage != null) {
                LOGGER.info("[Scheduler] " + targetDate + " için cover görseli oluşturuldu.");
            }

            InstagramImage concertImage = instagramService.createImage("concert", "post", "istanbul", targetDate);
            if (concertImage != null) {
                LOGGER.info("[Scheduler] " + targetDate + " için konser görseli oluşturuldu.");
            }

            InstagramImage theaterImage = instagramService.createImage("theater", "post", "istanbul", targetDate);
            if (theaterImage != null) {
                LOGGER.info("[Scheduler] " + targetDate + " için tiyatro görseli oluşturuldu.");
            }

            InstagramImage standupImage = instagramService.createImage("standup", "post", "istanbul", targetDate);
            if (standupImage != null) {
                LOGGER.info("[Scheduler] " + targetDate + " için stand-up görseli oluşturuldu.");
            }
        } catch (Exception e) {
            LOGGER.error("[Scheduler] " + targetDate + " için görsel oluşturma hatası", e);
        }
    }

    /**
     * Haftanın Pazartesi (Pazar createWeeklyPosts ile aynı hafta).
     * Pazar günü: yarınki Pazartesi; diğer günler: içinde bulunulan haftanın Pazartesi.
     */
    static LocalDate resolveFeedWeekMonday(LocalDate today) {
        if (today.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return today.plusDays(1);
        }
        return today.with(DayOfWeek.MONDAY);
    }

    /**
     * Manuel tetikleme (eski günlük karusel düzeni): ilgili haftanın 7 günü için her gün ayrı post (sırayla).
     */
    public List<String> shareFullWeekFeedPosts() throws InterruptedException {
        LocalDate monday = resolveFeedWeekMonday(LocalDate.now());
        List<LocalDate> week = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            week.add(monday.plusDays(i));
        }
        return sharePostsForDatesSequential(week, "Manuel (7 gün)");
    }

    /**
     * Belirli tarih aralığı (Pazartesi–Pazar veya kısmi) için sırayla günlük feed paylaşımı.
     */
    public List<String> shareFeedPostsForDateRange(LocalDate startInclusive, LocalDate endInclusive)
            throws InterruptedException {
        if (endInclusive.isBefore(startInclusive)) {
            throw new IllegalArgumentException("endDate startDate'den önce olamaz");
        }
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate d = startInclusive; !d.isAfter(endInclusive); d = d.plusDays(1)) {
            dates.add(d);
        }
        return sharePostsForDatesSequential(dates, "Manuel (tarih aralığı)");
    }

    /**
     * Tek cümle + hashtag yerine o günün gerçek etkinlik listesi (başlık • saat • mekân) + Kaydet
     * çağrısı — kaydetme, "haftanı planla" içeriğinde beğeniden daha güçlü bir erişim sinyali.
     * Etkinlik bulunamazsa (nadiren, görsel zaten üretilmişse olur) jenerik satıra düşer.
     */
    String buildDailyCaption(LocalDate targetDate) {
        List<Event> events = topEvents(targetDate, CAPTION_MAX_EVENTS);

        StringBuilder sb = new StringBuilder();
        sb.append("📅 ").append(targetDate.format(TR_DATE_YEAR_FORMAT)).append(" İstanbul Etkinlikleri 🎫\n\n");

        if (events.isEmpty()) {
            sb.append("🎭 Tiyatro | 🎶 Konser | 🎤 Stand-up\nBu günlerde şehirde neler var? Kaçırma!\n\n");
        } else {
            events.forEach(e -> sb.append(eventLine(e)));
            sb.append("\n🔖 Kaydet, hafta içinde lazım olacak!\n\n");
        }

        sb.append("🔗 Detaylar ve biletler profilde\n").append(hashtagsFor(events));
        return sb.toString();
    }

    /** O günün İstanbul'daki en iyi {@code limit} etkinliği (çocuk ve amatör geceler hariç, skor sırası). */
    private List<Event> topEvents(LocalDate date, int limit) {
        return eventService.findByDate(date.toString()).stream()
                .filter(e -> "istanbul".equalsIgnoreCase(e.getCity()))
                .filter(e -> !EventAudience.isChildrensEvent(e))
                .filter(e -> !EventQuality.isAmateurNight(e))
                .sorted(Comparator.comparingInt(scoringStrategy::calculateScore).reversed())
                .limit(limit)
                .toList();
    }

    /** "• Başlık · 21:00 @ Mekân" satırı. */
    private static String eventLine(Event e) {
        StringBuilder sb = new StringBuilder("• ").append(e.getTitle());
        if (e.getStartTime() != null) {
            sb.append(" · ").append(e.getStartTime().format(HHMM));
        }
        if (e.getVenue() != null && e.getVenue().getName() != null && !e.getVenue().getName().isBlank()) {
            sb.append(" @ ").append(e.getVenue().getName());
        }
        return sb.append('\n').toString();
    }

    private static String hashtagsFor(List<Event> events) {
        return Hashtags.build(events.stream()
                .map(e -> new Hashtags.Item(e.getType(), e.getTitle(),
                        e.getVenue() == null ? null : e.getVenue().getName(), e.getStartDate()))
                .toList());
    }

    private List<String> sharePostsForDatesSequential(List<LocalDate> dates, String batchName) throws InterruptedException {
        List<String> results = new ArrayList<>();
        LOGGER.info("[Scheduler] " + batchName + " — sırayla " + dates.size() + " günlük post...");
        for (int i = 0; i < dates.size(); i++) {
            results.addAll(sharePostsForSingleDate(dates.get(i), batchName + " [" + (i + 1) + "/" + dates.size() + "]"));
            if (i < dates.size() - 1) {
                Thread.sleep(DELAY_BETWEEN_DAILY_POSTS_MS);
            }
        }
        return results;
    }

    private List<String> sharePostsForSingleDate(LocalDate targetDate, String groupName) throws InterruptedException {
        List<String> results = new ArrayList<>();
        LOGGER.info("[Scheduler] " + groupName + " — " + targetDate + " için tek post gönderiliyor...");

        boolean coverShared = instagramImageRepository
                .existsByCreationDateAndPostTypeAndPostedToInstagramIsTrue(targetDate, "cover");
        if (coverShared) {
            String msg = "[Scheduler] " + targetDate + " için zaten cover paylaşılmış, atlanıyor.";
            LOGGER.info(msg);
            results.add(msg);
            return results;
        }

        List<InstagramImage> imagesToShare = instagramImageRepository
                .findByCreationDateAndPostedToInstagramFalse(targetDate);

        Map<String, InstagramImage> uniqueImages = new HashMap<>();
        for (InstagramImage img : imagesToShare) {
            if ("post".equals(img.getPostType()) || "cover".equals(img.getPostType())) {
                if (!uniqueImages.containsKey(img.getEventType())
                        || img.getId() > uniqueImages.get(img.getEventType()).getId()) {
                    uniqueImages.put(img.getEventType(), img);
                }
            }
        }
        List<InstagramImage> allPosts = new ArrayList<>(uniqueImages.values());

        if (allPosts.isEmpty()) {
            String msg = "[Scheduler] " + targetDate + " için paylaşılacak görsel yok.";
            LOGGER.info(msg);
            results.add(msg);
            return results;
        }

        allPosts.sort((img1, img2) -> {
            if ("cover".equals(img1.getEventType()) && !"cover".equals(img2.getEventType())) {
                return -1;
            }
            if (!"cover".equals(img1.getEventType()) && "cover".equals(img2.getEventType())) {
                return 1;
            }
            return Long.compare(img1.getId(), img2.getId());
        });

        LOGGER.info("[Scheduler] " + groupName + " paylaşım başlatılıyor. Görsel ID'leri: "
                + allPosts.stream().map(InstagramImage::getId).toList());

        try {
            // Event.venue LAZY ve cron thread'inde açık Hibernate oturumu yok — caption sorgusu kendi
            // transaction'ında (bu metot @Transactional olamaz: 10sn'lik bekleme döngüsü bağlantıyı tutardı).
            String caption = transactionTemplate.execute(status -> buildDailyCaption(targetDate));
            instagramService.sharePost(allPosts, caption);

            int maxAttempts = 30;
            boolean isPublished = false;
            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                Thread.sleep(10000);
                boolean checkShared = instagramImageRepository
                        .existsByCreationDateAndPostTypeAndPostedToInstagramIsTrue(targetDate, "cover");
                if (checkShared) {
                    isPublished = true;
                    break;
                }
                LOGGER.info("[Scheduler] " + targetDate + " paylaşımı bekleniyor... Deneme: "
                        + (attempt + 1) + "/" + maxAttempts);
            }

            if (isPublished) {
                String msg = "[Scheduler] " + targetDate + " paylaşımı tamamlandı.";
                LOGGER.info(msg);
                results.add(msg);
            } else {
                String msg = "[Scheduler] " + targetDate + " paylaşımı zaman aşımına uğradı veya başarısız oldu.";
                LOGGER.error(msg);
                results.add(msg);
            }
        } catch (Exception ex) {
            String err = "[Scheduler] HATA! " + targetDate + " paylaşımı: " + ex.getMessage();
            LOGGER.error(err, ex);
            results.add(err);
        }

        return results;
    }
} 