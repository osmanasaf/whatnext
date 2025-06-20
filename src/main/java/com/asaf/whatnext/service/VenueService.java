package com.asaf.whatnext.service;

import com.asaf.whatnext.models.Venue;
import com.asaf.whatnext.repository.VenueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class VenueService {
    private final VenueRepository venueRepository;

    @Autowired
    public VenueService(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    public Optional<Venue> findById(Long id) {
        return venueRepository.findById(id);
    }

    public Venue findByName(String name) {
        return venueRepository.findByName(name);
    }

    public Venue findOrCreateVenue(String name) {
        Venue venue = findByName(name);
        if (venue == null) {
            venue = new Venue();
            venue.setName(name);
            venue = save(venue);
        }
        return venue;
    }

    public Venue save(Venue venue) {
        return venueRepository.save(venue);
    }

    public void deleteById(Long id) {
        venueRepository.deleteById(id);
    }
}
