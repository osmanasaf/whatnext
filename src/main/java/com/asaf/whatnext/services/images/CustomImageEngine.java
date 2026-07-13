package com.asaf.whatnext.services.images;

import com.asaf.whatnext.services.ImageGenerationService;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

/**
 * "custom" motor — el yapımı şablon + ImageMagick, bugünkü davranış birebir korunur (varsayılan
 * motor, sıfır risk). Diğer iki motorun da fallback zincirinin son emniyet halkası.
 */
@Component
public class CustomImageEngine implements ImageRenderEngine {

    private final ImageGenerationService imageGenerationService;

    public CustomImageEngine(ImageGenerationService imageGenerationService) {
        this.imageGenerationService = imageGenerationService;
    }

    @Override
    public File render(ImageRenderRequest request) throws IOException, InterruptedException {
        String postType = request.postType();
        if ("cover".equals(postType)) {
            return imageGenerationService.createCover(request.date());
        }
        if ("story".equals(postType)) {
            return imageGenerationService.createSmartStory(request.date());
        }
        return imageGenerationService.createPost(request.eventType(), postType, request.city(), request.date());
    }
}
