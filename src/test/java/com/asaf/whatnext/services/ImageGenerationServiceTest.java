package com.asaf.whatnext.services;

import com.asaf.whatnext.dto.EventSummaryDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ImageGenerationServiceTest {

    @Test
    void buildVenueLineWithTime_prependsTimeWhenPresent() {
        EventSummaryDTO e = new EventSummaryDTO();
        e.setVenueName("Zorlu PSM");
        e.setStartTime(LocalTime.of(21, 0));
        assertThat(ImageGenerationService.buildVenueLineWithTime(e)).isEqualTo("21:00 • @Zorlu PSM");
    }

    @Test
    void buildVenueLineWithTime_noTime_venueOnly() {
        EventSummaryDTO e = new EventSummaryDTO();
        e.setVenueName("Zorlu PSM");
        assertThat(ImageGenerationService.buildVenueLineWithTime(e)).isEqualTo("@Zorlu PSM");
    }

    @Test
    void buildVenueLineWithTime_timeOnly_noVenue() {
        EventSummaryDTO e = new EventSummaryDTO();
        e.setStartTime(LocalTime.of(9, 30));
        assertThat(ImageGenerationService.buildVenueLineWithTime(e)).isEqualTo("09:30");
    }

    @Test
    void buildVenueLineWithTime_neitherTimeNorVenue_isBlank() {
        EventSummaryDTO e = new EventSummaryDTO();
        assertThat(ImageGenerationService.buildVenueLineWithTime(e)).isEmpty();
    }

    /**
     * Gerçek Aralık story şablonuyla uçtan uca render: gün+tarih alt satırı + saatli/saatsiz mekân
     * satırları görsel olarak doğrulanabilsin diye çıktı scratchpad'e de kopyalanır (ImageMagick
     * PATH'te değilse test atlanır — CI/geliştirme ortamı bağımsız).
     */
    @Test
    void buildImageMagickCommandAuto_rendersDayDateHeaderAndTimeLine(@TempDir Path tempDir) throws Exception {
        assumeTrue(isImageMagickAvailable(), "ImageMagick (magick) PATH'te değil, görsel render testi atlanıyor");

        File template = new File("src/main/resources/instagram-posts/december/story-template.png");
        assumeTrue(template.isFile(), "december/story-template.png bulunamadı");

        Properties coordinates = new Properties();
        coordinates.setProperty("layout.auto", "true");
        coordinates.setProperty("layout.region.left", "0.08");
        coordinates.setProperty("layout.region.top", "0.15");
        coordinates.setProperty("layout.region.width", "0.84");
        coordinates.setProperty("layout.region.height", "0.80");

        // Font.createFont Windows'ta dosya kilidini JVM çıkana kadar bırakmıyor (bilinen JDK davranışı —
        // üretim kodu bu yüzden zaten deleteOnExit kullanıyor); @TempDir yerine ayrı bir geçici dosyaya
        // kopyalanır ki JUnit'in dizin temizliği bu kilitle çakışıp testi başarısız göstermesin.
        File titleFont = copyClasspathResourceDeleteOnExit("AlfaSlabOne-Regular.ttf");
        File bodyFont = copyClasspathResourceDeleteOnExit("NotoSans-Regular.ttf");

        EventSummaryDTO withTime = new EventSummaryDTO();
        withTime.setTitle("Feridun Düzağaç");
        withTime.setVenueName("Dorock XL Kadıköy");
        withTime.setStartTime(LocalTime.of(21, 0));

        EventSummaryDTO withoutTime = new EventSummaryDTO();
        withoutTime.setTitle("Şehir Tiyatrosu: Bir Perde Bir Oyun");
        withoutTime.setVenueName("Harbiye Cemil Topuzlu Açıkhava Sahnesi");

        File outputFile = tempDir.resolve("december-story-test.png").toFile();

        List<String> command = ImageGenerationService.buildImageMagickCommandAuto(
                template.getAbsolutePath(),
                outputFile.getAbsolutePath(),
                "KONSER",
                List.of(withTime, withoutTime),
                coordinates,
                titleFont.getAbsolutePath(),
                bodyFont.getAbsolutePath(),
                1080, 1920,
                LocalDate.of(2026, 7, 17),
                true);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String log = new String(p.getInputStream().readAllBytes());
        int exit = p.waitFor();
        assertThat(exit).as("magick çıktısı: %s", log).isZero();
        assertThat(outputFile).exists();

        Path scratchpad = Path.of(System.getProperty("java.io.tmpdir"), "whatnext-test-preview");
        Files.createDirectories(scratchpad);
        Files.copy(outputFile.toPath(), scratchpad.resolve("december-story-daydate-time.png"),
                StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Render doğrulama çıktısı: " + scratchpad.resolve("december-story-daydate-time.png"));
    }

    /** Uzun başlıklarda AlfaSlabOne gibi geniş bir fontla gerçek sarma davranışı görsel doğrulama için. */
    @Test
    void buildImageMagickCommandAuto_wrapsLongTitlesWithoutOverflow(@TempDir Path tempDir) throws Exception {
        assumeTrue(isImageMagickAvailable(), "ImageMagick (magick) PATH'te değil, görsel render testi atlanıyor");

        File template = new File("src/main/resources/instagram-posts/december/story-template.png");
        assumeTrue(template.isFile(), "december/story-template.png bulunamadı");

        Properties coordinates = new Properties();
        coordinates.setProperty("layout.auto", "true");
        coordinates.setProperty("layout.region.left", "0.08");
        coordinates.setProperty("layout.region.top", "0.15");
        coordinates.setProperty("layout.region.width", "0.84");
        coordinates.setProperty("layout.region.height", "0.80");

        File titleFont = copyClasspathResourceDeleteOnExit("AlfaSlabOne-Regular.ttf");
        File bodyFont = copyClasspathResourceDeleteOnExit("NotoSans-Regular.ttf");

        List<EventSummaryDTO> events = new java.util.ArrayList<>();
        String[][] data = {
                {"Cumhurbaşkanlığı Senfoni Orkestrası: Beethoven'in 9. Senfonisi Özel Gecesi", "Cemal Reşit Rey Konser Salonu", "20:30"},
                {"Şehir Tiyatrosu: Bir Perde Bir Oyun", "Harbiye Cemil Topuzlu Açıkhava Sahnesi", null},
                {"Ankara Açık Mikrofon Stand-up Gecesi ve Sonrasında Sürpriz Konuk", "Kadıköy Sahne", "21:15"},
                {"Feridun Düzağaç", "Dorock XL Kadıköy", "21:00"},
                {"Gülinler", null, null},
        };
        for (String[] d : data) {
            EventSummaryDTO e = new EventSummaryDTO();
            e.setTitle(d[0]);
            e.setVenueName(d[1]);
            if (d[2] != null) {
                String[] hm = d[2].split(":");
                e.setStartTime(LocalTime.of(Integer.parseInt(hm[0]), Integer.parseInt(hm[1])));
            }
            events.add(e);
        }

        File outputFile = tempDir.resolve("december-story-longtitles.png").toFile();

        List<String> command = ImageGenerationService.buildImageMagickCommandAuto(
                template.getAbsolutePath(),
                outputFile.getAbsolutePath(),
                "KONSER",
                events,
                coordinates,
                titleFont.getAbsolutePath(),
                bodyFont.getAbsolutePath(),
                1080, 1920,
                LocalDate.of(2026, 7, 17),
                true);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String log = new String(p.getInputStream().readAllBytes());
        int exit = p.waitFor();
        assertThat(exit).as("magick çıktısı: %s", log).isZero();

        Path scratchpad = Path.of(System.getProperty("java.io.tmpdir"), "whatnext-test-preview");
        Files.createDirectories(scratchpad);
        Files.copy(outputFile.toPath(), scratchpad.resolve("december-story-longtitles.png"),
                StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Render doğrulama çıktısı: " + scratchpad.resolve("december-story-longtitles.png"));
    }

    private static boolean isImageMagickAvailable() {
        try {
            Process p = new ProcessBuilder("magick", "-version").start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static File copyClasspathResourceDeleteOnExit(String name) throws Exception {
        File out = File.createTempFile("whatnext-test-font-", "-" + name);
        out.deleteOnExit();
        try (InputStream in = ImageGenerationServiceTest.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalStateException("classpath resource yok: " + name);
            }
            Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return out;
    }
}
