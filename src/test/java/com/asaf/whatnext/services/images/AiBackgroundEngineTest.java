package com.asaf.whatnext.services.images;

import com.asaf.whatnext.services.templates.AiImageClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiBackgroundEngineTest {

    @Mock
    private AiImageClient aiImageClient;
    @Mock
    private RemotionStillEngine remotionStillEngine;

    private static final ImageRenderRequest REQUEST =
            new ImageRenderRequest("cover", "cover", null, LocalDate.of(2026, 7, 17));

    private static byte[] tinyPng() throws Exception {
        BufferedImage img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    @Test
    void firstCall_generatesAndCaches_thenDelegatesToRemotionWithBackground(@TempDir Path tempDir) throws Exception {
        Path cacheDir = tempDir.resolve("cache");
        Path videoDir = tempDir.resolve("video");
        when(aiImageClient.generateImage(anyString(), anyInt(), anyInt())).thenReturn(tinyPng());
        File expectedOut = new File("cover.png");
        when(remotionStillEngine.render(eq(REQUEST), anyString())).thenReturn(expectedOut);

        AiBackgroundEngine engine = new AiBackgroundEngine(
                aiImageClient, remotionStillEngine, cacheDir.toString(), videoDir.toString());

        File result = engine.render(REQUEST);

        assertThat(result).isEqualTo(expectedOut);
        verify(aiImageClient, times(1)).generateImage(anyString(), anyInt(), anyInt());
        assertThat(cacheDir.resolve("july").resolve("cover-ai-bg.png")).exists();
        assertThat(videoDir.resolve("public").resolve("ai-backgrounds").resolve("july-cover.png")).exists();

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        verify(remotionStillEngine).render(eq(REQUEST), pathCaptor.capture());
        assertThat(pathCaptor.getValue()).isEqualTo("ai-backgrounds/july-cover.png");
    }

    @Test
    void secondCall_usesCachedPng_doesNotCallAiClientAgain(@TempDir Path tempDir) throws Exception {
        Path cacheDir = tempDir.resolve("cache");
        Path videoDir = tempDir.resolve("video");
        when(aiImageClient.generateImage(anyString(), anyInt(), anyInt())).thenReturn(tinyPng());
        when(remotionStillEngine.render(eq(REQUEST), anyString())).thenReturn(new File("cover.png"));

        AiBackgroundEngine engine = new AiBackgroundEngine(
                aiImageClient, remotionStillEngine, cacheDir.toString(), videoDir.toString());

        engine.render(REQUEST);
        engine.render(REQUEST);

        verify(aiImageClient, times(1)).generateImage(anyString(), anyInt(), anyInt());
    }

    @Test
    void aiClientFails_fallsBackToRemotionWithoutBackground(@TempDir Path tempDir) throws Exception {
        Path cacheDir = tempDir.resolve("cache");
        Path videoDir = tempDir.resolve("video");
        when(aiImageClient.generateImage(anyString(), anyInt(), anyInt()))
                .thenThrow(new java.io.IOException("whatnext.templates.ai.api-key ayarlanmamış"));
        File fallbackOut = new File("cover-fallback.png");
        when(remotionStillEngine.render(eq(REQUEST), isNull())).thenReturn(fallbackOut);

        AiBackgroundEngine engine = new AiBackgroundEngine(
                aiImageClient, remotionStillEngine, cacheDir.toString(), videoDir.toString());

        File result = engine.render(REQUEST);

        // remotionStillEngine yalnızca (REQUEST, null) ile stub'landı — sonuç fallbackOut ise
        // arka plan olmadan (palet gradyanına düşerek) çağrıldığı kanıtlanmış olur.
        assertThat(result).isEqualTo(fallbackOut);
    }
}
