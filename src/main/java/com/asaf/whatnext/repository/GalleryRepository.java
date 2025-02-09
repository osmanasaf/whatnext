package com.asaf.whatnext.repository;

import com.asaf.whatnext.models.Gallery;

public interface GalleryRepository extends BaseRepository<Gallery, Long> {
    Gallery findByName(String name);
}
