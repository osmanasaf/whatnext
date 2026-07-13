package com.asaf.whatnext.services.images;

import com.asaf.whatnext.services.templates.AiImageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;

/**
 * "ai" motoru — yalnızca ARKA PLANI Gemini'den üretir, metni asla AI basmaz (Türkçe karakter/kontrol
 * riski + stil tutarlılığı). Üretilen arka plan {@code backgroundImageUrl} prop'uyla
 * {@link RemotionStillEngine}'e devredilir — metin katmanı, layout, marka fontları hep Remotion'da.
 * Ay+tür başına bir kez üretilip önbelleklenir (12 ay × birkaç tür ≈ yıllık birkaç dolar; her koşuda
 * üretmek hem gereksiz maliyet hem stil tutarsızlığı olurdu). API anahtarı yoksa/hata verirse
 * paletli {@link RemotionStillEngine}'e (arka plan olmadan) düşülür — 11:50 cron'u asla bloklanmaz.
 */
@Component
public class AiBackgroundEngine implements ImageRenderEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiBackgroundEngine.class);
    private static final Map<String, int[]> DIMENSIONS = Map.of(
            "post", new int[]{1024, 1024},
            "cover", new int[]{1024, 1024},
            "story", new int[]{1080, 1920});

    private final AiImageClient aiImageClient;
    private final RemotionStillEngine remotionStillEngine;
    private final File cacheDir;
    private final File videoPublicDir;

    public AiBackgroundEngine(
            AiImageClient aiImageClient,
            RemotionStillEngine remotionStillEngine,
            @Value("${whatnext.images.ai.cache-dir:generated-backgrounds}") String cacheDirPath,
            @Value("${whatnext.video.project-path:video}") String videoProjectPath) {
        this.aiImageClient = aiImageClient;
        this.remotionStillEngine = remotionStillEngine;
        this.cacheDir = new File(cacheDirPath);
        this.videoPublicDir = new File(new File(videoProjectPath, "public"), "ai-backgrounds");
    }

    @Override
    public File render(ImageRenderRequest request) throws IOException, InterruptedException {
        String monthName = request.date().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();
        String kind = request.postType();
        try {
            File cachedPng = resolveOrGenerateBackground(monthName, kind);
            String staticRelativePath = copyIntoVideoPublic(cachedPng, monthName, kind);
            return remotionStillEngine.render(request, staticRelativePath);
        } catch (Exception e) {
            LOGGER.warn("AI arka plan üretimi başarısız ({} {}), paletli remotion motoruna düşülüyor: {}",
                    monthName, kind, e.getMessage());
            return remotionStillEngine.render(request, null);
        }
    }

    private File resolveOrGenerateBackground(String monthName, String kind) throws IOException {
        File monthDir = new File(cacheDir, monthName);
        Files.createDirectories(monthDir.toPath());
        File pngFile = new File(monthDir, kind + "-ai-bg.png");
        if (pngFile.isFile()) {
            return pngFile;
        }

        int[] dims = DIMENSIONS.getOrDefault(kind, DIMENSIONS.get("post"));
        String prompt = buildPrompt(monthName);
        byte[] imageBytes = aiImageClient.generateImage(prompt, dims[0], dims[1]);
        BufferedImage raw = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (raw == null) {
            throw new IOException("AI sağlayıcısı geçersiz görsel döndürdü");
        }
        ImageIO.write(raw, "png", pngFile);
        return pngFile;
    }

    private String copyIntoVideoPublic(File cachedPng, String monthName, String kind) throws IOException {
        Files.createDirectories(videoPublicDir.toPath());
        String fileName = monthName + "-" + kind + ".png";
        File dest = new File(videoPublicDir, fileName);
        Files.copy(cachedPng.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return "ai-backgrounds/" + fileName;
    }

    private static String buildPrompt(String monthName) {
        return "Minimal abstract poster background for a " + monthName + " event listing in Istanbul, "
                + "soft gradient, warm inviting palette, no text, no people, no logos, "
                + "elegant negative space in the center-left area for a title and event list";
    }
}
