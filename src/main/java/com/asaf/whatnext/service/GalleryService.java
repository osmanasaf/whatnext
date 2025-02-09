package com.asaf.whatnext.service;

import com.asaf.whatnext.models.Gallery;
import com.asaf.whatnext.repository.GalleryRepository;
import org.springframework.stereotype.Service;

@Service
public class GalleryService extends BaseService<Gallery, Long> {

    public GalleryService(GalleryRepository repository) {
        super(repository);
    }

    public Gallery findByName(String name) {
        return ((GalleryRepository) repository).findByName(name);
    }
}
