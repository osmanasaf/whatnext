package com.asaf.whatnext.services.images;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DelegatingImageEngineTest {

    @Mock
    private CustomImageEngine customImageEngine;
    @Mock
    private RemotionStillEngine remotionStillEngine;
    @Mock
    private AiBackgroundEngine aiBackgroundEngine;

    private final ImageRenderRequest request =
            new ImageRenderRequest("concert", "post", "istanbul", LocalDate.of(2026, 7, 17));

    @Test
    void engineCustom_goesDirectlyToCustom_noFallbackAttempted() throws Exception {
        File expected = new File("custom.png");
        when(customImageEngine.render(request)).thenReturn(expected);

        DelegatingImageEngine delegating = new DelegatingImageEngine(
                "custom", customImageEngine, remotionStillEngine, aiBackgroundEngine);

        File result = delegating.render(request);

        assertThat(result).isEqualTo(expected);
        verify(remotionStillEngine, never()).render(any());
        verify(aiBackgroundEngine, never()).render(any());
    }

    @Test
    void engineRemotion_usesRemotion_whenSuccessful() throws Exception {
        File expected = new File("remotion.png");
        when(remotionStillEngine.render(request)).thenReturn(expected);

        DelegatingImageEngine delegating = new DelegatingImageEngine(
                "remotion", customImageEngine, remotionStillEngine, aiBackgroundEngine);

        File result = delegating.render(request);

        assertThat(result).isEqualTo(expected);
        verify(customImageEngine, never()).render(any());
    }

    @Test
    void engineRemotion_fallsBackToCustom_onFailure() throws Exception {
        when(remotionStillEngine.render(request)).thenThrow(new IOException("node bulunamadı"));
        File fallback = new File("custom-fallback.png");
        when(customImageEngine.render(request)).thenReturn(fallback);

        DelegatingImageEngine delegating = new DelegatingImageEngine(
                "remotion", customImageEngine, remotionStillEngine, aiBackgroundEngine);

        File result = delegating.render(request);

        assertThat(result).isEqualTo(fallback);
    }

    @Test
    void engineAi_fallsBackToCustom_onFailure() throws Exception {
        when(aiBackgroundEngine.render(request)).thenThrow(new IOException("Gemini API hatası"));
        File fallback = new File("custom-fallback.png");
        when(customImageEngine.render(request)).thenReturn(fallback);

        DelegatingImageEngine delegating = new DelegatingImageEngine(
                "ai", customImageEngine, remotionStillEngine, aiBackgroundEngine);

        File result = delegating.render(request);

        assertThat(result).isEqualTo(fallback);
    }

    @Test
    void unknownEngineValue_defaultsToCustom() throws Exception {
        File expected = new File("custom.png");
        when(customImageEngine.render(request)).thenReturn(expected);

        DelegatingImageEngine delegating = new DelegatingImageEngine(
                "typo", customImageEngine, remotionStillEngine, aiBackgroundEngine);

        assertThat(delegating.render(request)).isEqualTo(expected);
    }
}
