package com.asaf.whatnext.services.images;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * {@code whatnext.images.engine} bayrağına göre custom|remotion|ai motoruna yönlendirir.
 * Fallback: {@code ai}/{@code remotion} başarısız olursa (node yok, API hatası, vb.) {@code custom}'a
 * düşülür — custom zaten bugünkü davranışı birebir koruyan varsayılan motor, en güvenli emniyet
 * halkası. {@link AiBackgroundEngine} kendi içinde ai→remotion adımını zaten yapıyor; burası sadece
 * dıştaki son emniyet halkasını (→custom) ekliyor. 11:50 cron'u hiçbir koşulda bloklanmaz.
 */
@Component
@Primary
public class DelegatingImageEngine implements ImageRenderEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(DelegatingImageEngine.class);

    private final String engine;
    private final CustomImageEngine customImageEngine;
    private final RemotionStillEngine remotionStillEngine;
    private final AiBackgroundEngine aiBackgroundEngine;

    public DelegatingImageEngine(
            @Value("${whatnext.images.engine:custom}") String engine,
            CustomImageEngine customImageEngine,
            RemotionStillEngine remotionStillEngine,
            AiBackgroundEngine aiBackgroundEngine) {
        this.engine = engine == null ? "custom" : engine.trim().toLowerCase(Locale.ROOT);
        this.customImageEngine = customImageEngine;
        this.remotionStillEngine = remotionStillEngine;
        this.aiBackgroundEngine = aiBackgroundEngine;
    }

    @Override
    public File render(ImageRenderRequest request) throws IOException, InterruptedException {
        return switch (engine) {
            case "ai" -> renderWithFallback(aiBackgroundEngine, "ai", request);
            case "remotion" -> renderWithFallback(remotionStillEngine, "remotion", request);
            default -> customImageEngine.render(request);
        };
    }

    private File renderWithFallback(ImageRenderEngine primary, String engineName, ImageRenderRequest request)
            throws IOException, InterruptedException {
        try {
            return primary.render(request);
        } catch (Exception e) {
            LOGGER.warn("\"{}\" motoru başarısız, custom motoruna düşülüyor: {}", engineName, e.getMessage());
            return customImageEngine.render(request);
        }
    }
}
