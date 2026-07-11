package com.asaf.whatnext.services.templates;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Properties;

/**
 * Parametrik (Java2D) şablon üretici: mevcut 36 el yapımı PNG'den örneklenen ay paletleriyle
 * gradyan arka plan + dekoratif öğe üretir; motorun otomatik yerleşim modunu ({@code layout.auto=true})
 * kullanan bir koordinat dosyasıyla eşleştirir. Çıktı {@code whatnext.templates.workdir} altında
 * önbelleklenir (palet değişmedikçe yeniden üretilmez).
 */
@Component
public class GeneratedTemplateProvider implements TemplateProvider {

    private static final Map<String, int[]> DIMENSIONS = Map.of(
            "post", new int[]{1024, 1024},
            "cover", new int[]{1024, 1024},
            "story", new int[]{1024, 1536});

    private final File workdir;
    private final Properties style;

    public GeneratedTemplateProvider(
            @Value("${whatnext.templates.workdir:generated-templates}") String workdirPath) throws IOException {
        this.workdir = new File(workdirPath);
        this.style = loadStyle();
    }

    @Override
    public TemplateAssets resolve(String monthName, String postType) throws IOException {
        File monthDir = new File(workdir, monthName);
        Files.createDirectories(monthDir.toPath());
        File pngFile = new File(monthDir, postType + "-template.png");
        File coordFile = new File(monthDir, postType + "-coordinates.properties");
        File hashFile = new File(monthDir, postType + ".hash");

        Palette palette = paletteFor(monthName);
        String hash = postType + "|" + palette.bg1() + "|" + palette.bg2() + "|" + palette.accent() + "|" + palette.text();

        boolean cached = pngFile.isFile() && coordFile.isFile() && hashFile.isFile()
                && hash.equals(readHash(hashFile));
        if (!cached) {
            int[] dims = DIMENSIONS.getOrDefault(postType, DIMENSIONS.get("post"));
            renderBackground(pngFile, dims[0], dims[1], palette);
            writeCoordinates(coordFile, postType, palette);
            Files.writeString(hashFile.toPath(), hash, StandardCharsets.UTF_8);
        }

        return new TemplateAssets(pngFile, loadCoordinates(coordFile));
    }

    private void renderBackground(File out, int w, int h, Palette palette) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        g.setPaint(new GradientPaint(0, 0, palette.bg1(), w, h, palette.bg2()));
        g.fillRect(0, 0, w, h);

        // Dekoratif yumuşak leke (mevcut şablonlardaki organik doku hissini taklit eder)
        float blobR = w * 0.65f;
        RadialGradientPaint blob = new RadialGradientPaint(
                w * 0.82f, h * 0.14f, blobR,
                new float[]{0f, 1f},
                new Color[]{withAlpha(palette.accent(), 60), withAlpha(palette.accent(), 0)});
        g.setPaint(blob);
        g.fill(new Ellipse2D.Float(w * 0.82f - blobR, h * 0.14f - blobR, blobR * 2, blobR * 2));

        float blob2R = w * 0.5f;
        RadialGradientPaint blob2 = new RadialGradientPaint(
                w * 0.1f, h * 0.92f, blob2R,
                new float[]{0f, 1f},
                new Color[]{withAlpha(palette.bg1(), 90), withAlpha(palette.bg1(), 0)});
        g.setPaint(blob2);
        g.fill(new Ellipse2D.Float(w * 0.1f - blob2R, h * 0.92f - blob2R, blob2R * 2, blob2R * 2));

        g.dispose();

        Files.createDirectories(out.getParentFile().toPath());
        ImageIO.write(img, "png", out);
    }

    private static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    /** Package-private: {@link AiTemplateProvider} de aynı auto-layout koordinat kuralını kullanır. */
    void writeCoordinates(File coordFile, String postType, Palette palette) throws IOException {
        StringBuilder sb = new StringBuilder();
        if ("cover".equals(postType)) {
            sb.append("date.pointsize=80\n");
            sb.append("date.geometry=+0+0\n");
        } else {
            sb.append("layout.auto=true\n");
            sb.append("layout.region.left=0.16\n");
            sb.append("layout.region.top=0.24\n");
            sb.append("layout.region.width=0.68\n");
            sb.append("layout.region.height=0.62\n\n");
            sb.append("event_type.fill=").append(toHex(palette.accent())).append('\n');
            sb.append("venue.fill=").append(toHex(palette.text())).append('\n');
        }
        Files.writeString(coordFile.toPath(), sb.toString(), StandardCharsets.UTF_8);
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static String readHash(File hashFile) {
        try {
            return Files.readString(hashFile.toPath(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return "";
        }
    }

    Palette paletteFor(String monthName) {
        String prefix = "palette." + monthName + ".";
        return new Palette(
                colorProp(prefix + "bg1", "#cccccc"),
                colorProp(prefix + "bg2", "#999999"),
                colorProp(prefix + "accent", "#783c32"),
                colorProp(prefix + "text", "#1a1a1a"));
    }

    private Color colorProp(String key, String defaultHex) {
        String v = style.getProperty(key, defaultHex).trim();
        return Color.decode(v);
    }

    private static Properties loadStyle() throws IOException {
        Properties p = new Properties();
        try (InputStream in = GeneratedTemplateProvider.class.getClassLoader()
                .getResourceAsStream("instagram-templates/style.properties")) {
            if (in != null) {
                p.load(new StringReader(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
            }
        }
        return p;
    }

    private static Properties loadCoordinates(File file) throws IOException {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file.toPath())) {
            p.load(new StringReader(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
        }
        return p;
    }

    record Palette(Color bg1, Color bg2, Color accent, Color text) {
    }
}
