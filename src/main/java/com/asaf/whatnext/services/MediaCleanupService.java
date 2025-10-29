package com.asaf.whatnext.services;

import com.asaf.whatnext.models.InstagramImage;
import com.asaf.whatnext.repository.InstagramImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class MediaCleanupService {

    private final InstagramImageRepository instagramImageRepository;
    private final CloudinaryService cloudinaryService;

    @Autowired
    public MediaCleanupService(InstagramImageRepository instagramImageRepository,
                               CloudinaryService cloudinaryService) {
        this.instagramImageRepository = instagramImageRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Europe/Istanbul")
    public void deleteOldImages() {
        LocalDate threshold = LocalDate.now().minusDays(14);
        List<InstagramImage> oldImages = instagramImageRepository.findAll().stream()
                .filter(img -> img.getCreationDate() != null && img.getCreationDate().isBefore(threshold))
                .toList();

        for (InstagramImage image : oldImages) {
            try {
                String publicId = image.getCloudinaryPublicId();
                if (publicId != null && !publicId.isEmpty()) {
                    boolean deleted = cloudinaryService.deleteByPublicId(publicId);
                    if (deleted) {
                        instagramImageRepository.delete(image);
                    }
                } else {
                    instagramImageRepository.delete(image);
                }
            } catch (Exception ignored) {}
        }
    }
}


