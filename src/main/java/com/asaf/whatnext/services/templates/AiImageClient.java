package com.asaf.whatnext.services.templates;

import java.io.IOException;

/**
 * Bir metin prompt'undan ham görsel byte'ları üreten ince arayüz — sağlayıcı (Gemini/OpenAI/Stability)
 * değişse de {@link AiTemplateProvider} etkilenmez.
 */
public interface AiImageClient {
    byte[] generateImage(String prompt, int width, int height) throws IOException;
}
