package com.asaf.whatnext.services.templates;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratedTemplateProviderTest {

    @Test
    void resolvePost_rendersCorrectDimensionsAndAutoLayoutCoordinates(@TempDir Path tempDir) throws Exception {
        GeneratedTemplateProvider provider = new GeneratedTemplateProvider(tempDir.toString());

        TemplateAssets assets = provider.resolve("july", "post");

        assertThat(assets.templatePng()).exists();
        BufferedImage img = ImageIO.read(assets.templatePng());
        assertThat(img.getWidth()).isEqualTo(1024);
        assertThat(img.getHeight()).isEqualTo(1024);
        assertThat(assets.coordinates().getProperty("layout.auto")).isEqualTo("true");
        assertThat(assets.coordinates().getProperty("layout.region.left")).isNotBlank();
    }

    @Test
    void resolveStory_isTallerCanvas(@TempDir Path tempDir) throws Exception {
        GeneratedTemplateProvider provider = new GeneratedTemplateProvider(tempDir.toString());

        TemplateAssets assets = provider.resolve("december", "story");

        BufferedImage img = ImageIO.read(assets.templatePng());
        assertThat(img.getWidth()).isEqualTo(1024);
        assertThat(img.getHeight()).isEqualTo(1536);
    }

    @Test
    void resolveCover_writesDatePointsizeAndGeometryInsteadOfAutoLayout(@TempDir Path tempDir) throws Exception {
        GeneratedTemplateProvider provider = new GeneratedTemplateProvider(tempDir.toString());

        TemplateAssets assets = provider.resolve("march", "cover");

        assertThat(assets.coordinates().getProperty("date.pointsize")).isNotBlank();
        assertThat(assets.coordinates().getProperty("date.geometry")).isNotBlank();
        assertThat(assets.coordinates().getProperty("layout.auto")).isNull();
    }

    @Test
    void resolve_isCachedOnSecondCall(@TempDir Path tempDir) throws Exception {
        GeneratedTemplateProvider provider = new GeneratedTemplateProvider(tempDir.toString());

        TemplateAssets first = provider.resolve("may", "post");
        long firstModified = first.templatePng().lastModified();
        TemplateAssets second = provider.resolve("may", "post");

        assertThat(second.templatePng()).isEqualTo(first.templatePng());
        assertThat(second.templatePng().lastModified()).isEqualTo(firstModified);
    }
}
