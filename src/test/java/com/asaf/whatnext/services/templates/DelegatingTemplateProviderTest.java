package com.asaf.whatnext.services.templates;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DelegatingTemplateProviderTest {

    @Mock
    private StaticTemplateProvider staticProvider;
    @Mock
    private GeneratedTemplateProvider generatedProvider;
    @Mock
    private AiTemplateProvider aiProvider;

    private static TemplateAssets assets(String tag) {
        return new TemplateAssets(new File(tag), new Properties());
    }

    @Test
    void defaultMode_static_alwaysUsesStaticProvider() throws IOException {
        when(staticProvider.resolve("july", "post")).thenReturn(assets("static"));
        DelegatingTemplateProvider provider = new DelegatingTemplateProvider(
                staticProvider, generatedProvider, aiProvider, "static");

        TemplateAssets result = provider.resolve("july", "post");

        assertThat(result.templatePng().getName()).isEqualTo("static");
        verify(generatedProvider, never()).resolve(anyString(), anyString());
    }

    @Test
    void modeGenerated_usesGeneratedProvider() throws IOException {
        when(generatedProvider.resolve("july", "post")).thenReturn(assets("generated"));
        DelegatingTemplateProvider provider = new DelegatingTemplateProvider(
                staticProvider, generatedProvider, aiProvider, "generated");

        TemplateAssets result = provider.resolve("july", "post");

        assertThat(result.templatePng().getName()).isEqualTo("generated");
    }

    @Test
    void modeAi_fallsBackToStaticWhenAiProviderThrows() throws IOException {
        when(aiProvider.resolve("july", "post")).thenThrow(new IOException("no api key"));
        when(staticProvider.resolve("july", "post")).thenReturn(assets("static-fallback"));
        DelegatingTemplateProvider provider = new DelegatingTemplateProvider(
                staticProvider, generatedProvider, aiProvider, "ai");

        TemplateAssets result = provider.resolve("july", "post");

        assertThat(result.templatePng().getName()).isEqualTo("static-fallback");
    }

    @Test
    void unknownMode_defaultsToStatic() throws IOException {
        when(staticProvider.resolve("july", "post")).thenReturn(assets("static"));
        DelegatingTemplateProvider provider = new DelegatingTemplateProvider(
                staticProvider, generatedProvider, aiProvider, "totally-invalid-mode");

        TemplateAssets result = provider.resolve("july", "post");

        assertThat(result.templatePng().getName()).isEqualTo("static");
    }
}
