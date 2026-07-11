package com.asaf.whatnext.services.templates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Locale;

/**
 * {@code whatnext.templates.mode=static|generated|ai} (varsayılan {@code static} — mevcut davranış
 * birebir korunur, hiçbir maliyet/dış bağımlılık eklenmez). {@code generated}/{@code ai} modunda
 * seçilen sağlayıcı hata verirse (AI anahtarı yok, sınıf yolunda kaynak eksik vb.) her zaman
 * classpath {@link StaticTemplateProvider}'a düşülür — görsel üretimi asla tamamen bloklanmaz.
 */
@Component
@Primary
public class DelegatingTemplateProvider implements TemplateProvider {

    private static final Logger LOG = LoggerFactory.getLogger(DelegatingTemplateProvider.class);

    private final StaticTemplateProvider staticProvider;
    private final GeneratedTemplateProvider generatedProvider;
    private final AiTemplateProvider aiProvider;
    private final String mode;

    public DelegatingTemplateProvider(
            StaticTemplateProvider staticProvider,
            GeneratedTemplateProvider generatedProvider,
            AiTemplateProvider aiProvider,
            @Value("${whatnext.templates.mode:static}") String mode) {
        this.staticProvider = staticProvider;
        this.generatedProvider = generatedProvider;
        this.aiProvider = aiProvider;
        this.mode = mode == null ? "static" : mode.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public TemplateAssets resolve(String monthName, String postType) throws IOException {
        TemplateProvider selected = switch (mode) {
            case "generated" -> generatedProvider;
            case "ai" -> aiProvider;
            default -> staticProvider;
        };
        if (selected == staticProvider) {
            return staticProvider.resolve(monthName, postType);
        }
        try {
            return selected.resolve(monthName, postType);
        } catch (Exception e) {
            LOG.warn("whatnext.templates.mode={} çözümü başarısız ({} {}), static fallback: {}",
                    mode, monthName, postType, e.getMessage());
            return staticProvider.resolve(monthName, postType);
        }
    }
}
