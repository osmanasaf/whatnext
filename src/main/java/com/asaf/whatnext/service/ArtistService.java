package com.asaf.whatnext.service;

import com.asaf.whatnext.models.Artist;
import com.asaf.whatnext.repository.ArtistRepository;
import org.springframework.stereotype.Service;

@Service
public class ArtistService extends BaseService<Artist, Long> {

    public ArtistService(ArtistRepository repository) {
        super(repository);
    }

    public Artist findByName(String name) {
        return ((ArtistRepository) repository).findByName(name);
    }
}
