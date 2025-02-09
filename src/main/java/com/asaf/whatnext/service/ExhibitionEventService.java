package com.asaf.whatnext.service;

import com.asaf.whatnext.models.ExhibitionEvent;
import com.asaf.whatnext.repository.ExhibitionEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExhibitionEventService extends BaseService<ExhibitionEvent, Long> {

    public ExhibitionEventService(ExhibitionEventRepository repository) {
        super(repository);
    }

    public List<ExhibitionEvent> findByArtist(Long artistId) {
        return ((ExhibitionEventRepository) repository).findByArtistId(artistId);
    }

    public List<ExhibitionEvent> findByGallery(Long galleryId) {
        return ((ExhibitionEventRepository) repository).findByGalleryId(galleryId);
    }
}
