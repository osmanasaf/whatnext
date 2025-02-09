package com.asaf.whatnext.service;

import com.asaf.whatnext.models.Venue;
import com.asaf.whatnext.repository.VenueRepository;
import org.springframework.stereotype.Service;

@Service
public class VenueService extends BaseService<Venue, Long> {

    public VenueService(VenueRepository repository) {
        super(repository);
    }

    public Venue findByName(String name) {
        return ((VenueRepository) repository).findByName(name);
    }
}
