package com.asaf.whatnext.repository;

import com.asaf.whatnext.models.ExhibitionEvent;

import java.util.List;

public interface ExhibitionEventRepository extends BaseRepository<ExhibitionEvent, Long> {
    List<ExhibitionEvent> findByArtistId(Long artistId);
    List<ExhibitionEvent> findByGalleryId(Long galleryId);
}
