package com.asaf.whatnext.services.templates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Properties;

/**
 * AI görsel sağlayıcısından ay başına soyut arka plan üretir; aynı çıktı sözleşmesi
 * ({@code layout.auto=true} koordinatları) — kalan boru hattı (ImageMagick, Cloudinary, scheduler) dokunulmaz.
 * API hatasında (anahtar yok, ağ hatası, kota) sessizce {@link GeneratedTemplateProvider}'a düşer —
 * 11:50 cron'u AI sağlayıcısı çöktüğünde asla bloklanmaz.
 */
@Component
public class AiTemplateProvider implements TemplateProvider {

    private static final Logger LOG = LoggerFactory.getLogger(AiTemplateProvider.class);
    private static final Map<String, int[]> DIMENSIONS = Map.of(
            "post", new int[]{1024, 1024},
            "cover", new int[]{1024, 1024},
            "story", new int[]{1024, 1536});

    private final AiImageClient aiImageClient;
    private final GeneratedTemplateProvider fallback;
    private final File workdir;

    public AiTemplateProvider(
            AiImageClient aiImageClient,
            GeneratedTemplateProvider fallback,
            @Value("${whatnext.templates.workdir:generated-templates}") String workdirPath) {
        this.aiImageClient = aiImageClient;
        this.fallback = fallback;
        this.workdir = new File(workdirPath);
    }

    @Override
    public TemplateAssets resolve(String monthName, String postType) throws IOException {
        try {
            return resolveFromAi(monthName, postType);
        } catch (Exception e) {
            LOG.warn("AI şablon üretimi başarısız ({} {}), generated'a düşülüyor: {}", monthName, postType, e.getMessage());
            return fallback.resolve(monthName, postType);
        }
    }

    private TemplateAssets resolveFromAi(String monthName, String postType) throws IOException {
        File monthDir = new File(new File(workdir, "ai"), monthName);
        Files.createDirectories(monthDir.toPath());
        File pngFile = new File(monthDir, postType + "-template.png");
        File coordFile = new File(monthDir, postType + "-coordinates.properties");

        int[] dims = DIMENSIONS.getOrDefault(postType, DIMENSIONS.get("post"));
        String prompt = buildPrompt(monthName);

        byte[] imageBytes = aiImageClient.generateImage(prompt, dims[0], dims[1]);
        BufferedImage raw = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (raw == null) {
            throw new IOException("AI sağlayıcısı geçersiz görsel döndürdü");
        }
        BufferedImage canvas = cropToFill(raw, dims[0], dims[1]);
        ImageIO.write(canvas, "png", pngFile);

        GeneratedTemplateProvider.Palette palette = fallback.paletteFor(monthName);
        fallback.writeCoordinates(coordFile, postType, palette);

        Properties coordinates = new Properties();
        try (var in = Files.newInputStream(coordFile.toPath())) {
            coordinates.load(in);
        }
        return new TemplateAssets(pngFile, coordinates);
    }

    private static String buildPrompt(String monthName) {
        return "Minimal abstract poster background for a " + monthName + " event listing, "
                + "soft gradient, no text, no people, no logos, elegant negative space in the center-left area";
    }

    /** Dönen görsel tam tuvale kırpılır (merkezden, en-boy oranını koruyarak). */
    private static BufferedImage cropToFill(BufferedImage src, int targetW, int targetH) {
        double srcRatio = (double) src.getWidth() / src.getHeight();
        double targetRatio = (double) targetW / targetH;

        int cropW, cropH, cropX, cropY;
        if (srcRatio > targetRatio) {
            cropH = src.getHeight();
            cropW = (int) Math.round(cropH * targetRatio);
            cropX = (src.getWidth() - cropW) / 2;
            cropY = 0;
        } else {
            cropW = src.getWidth();
            cropH = (int) Math.round(cropW / targetRatio);
            cropX = 0;
            cropY = (src.getHeight() - cropH) / 2;
        }
        BufferedImage cropped = src.getSubimage(cropX, cropY, cropW, cropH);

        BufferedImage scaled = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(cropped, 0, 0, targetW, targetH, null);
        g.dispose();
        return scaled;
    }
}
