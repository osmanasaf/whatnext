package com.asaf.whatnext.config;

import com.asaf.whatnext.models.Venue;
import com.asaf.whatnext.models.Artist;
import com.asaf.whatnext.models.Director;
import com.asaf.whatnext.models.Actor;
import com.asaf.whatnext.repository.VenueRepository;
import com.asaf.whatnext.repository.ArtistRepository;
import com.asaf.whatnext.repository.DirectorRepository;
import com.asaf.whatnext.repository.ActorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
public class PopularitySeeder implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PopularitySeeder.class);
    private static final int HIGH_PRIORITY_SCORE = 10;
    private static final int MEDIUM_PRIORITY_SCORE = 5;

    private final VenueRepository venueRepository;
    private final ArtistRepository artistRepository;
    private final DirectorRepository directorRepository;
    private final ActorRepository actorRepository;
    private final ObjectMapper objectMapper;

    public PopularitySeeder(VenueRepository venueRepository, ArtistRepository artistRepository,
                           DirectorRepository directorRepository, ActorRepository actorRepository) {
        this.venueRepository = venueRepository;
        this.artistRepository = artistRepository;
        this.directorRepository = directorRepository;
        this.actorRepository = actorRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Transactional
    public void run(String... args) {
        try {
            PopularityData data = loadPopularityData();
            seedVenueScoresFromList(data.getTopVenues());
            seedArtistScoresFromList(data.getTopArtists());
        } catch (IOException e) {
            LOGGER.error("Failed to load popular_config.json", e);
        }

        seedTopVenuesFromDB();
        seedTopArtistsFromDB();
        seedTopDirectorsFromDB();
        seedTopActorsFromDB();
    }

    private PopularityData loadPopularityData() throws IOException {
        ClassPathResource resource = new ClassPathResource("popular_config.json");
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, PopularityData.class);
        }
    }

    private void seedVenueScoresFromList(List<String> topVenues) {
        if (topVenues == null) return;
        for (String venueName : topVenues) {
            List<Venue> venues = venueRepository.findByNameContainingIgnoreCase(venueName.trim());
            for (Venue venue : venues) {
                updateVenueScore(venue, HIGH_PRIORITY_SCORE);
            }
        }
    }
    
    private void seedArtistScoresFromList(List<String> topArtists) {
        if (topArtists == null) return;
        for (String artistName : topArtists) {
             List<Artist> artists = artistRepository.findByNameContainingIgnoreCase(artistName.trim());
             for (Artist artist : artists) {
                updateArtistScore(artist, HIGH_PRIORITY_SCORE);
             }
        }
    }
    
    private void seedTopVenuesFromDB() {
        try {
            List<Venue> topVenues = venueRepository.findTopVenuesByEventCount(PageRequest.of(0, 20));
            for (Venue venue : topVenues) {
                 updateVenueScore(venue, MEDIUM_PRIORITY_SCORE);
            }
        } catch (Exception e) {
            LOGGER.error("Error seeding venues from DB", e);
        }
    }
    
    private void seedTopArtistsFromDB() {
        try {
            List<Artist> topArtists = artistRepository.findTopArtistsByEventCount(PageRequest.of(0, 50));
            for (Artist artist : topArtists) {
                 updateArtistScore(artist, MEDIUM_PRIORITY_SCORE);
            }
        } catch (Exception e) {
             LOGGER.error("Error seeding artists from DB", e);
        }
    }

    private void updateVenueScore(Venue venue, int score) {
        if (venue.getPopularityScore() < score) {
            venue.setPopularityScore(score);
            venueRepository.save(venue);
            LOGGER.info("Updated venue popularity score: {} -> {}", venue.getName(), score);
        }
    }

    private void updateArtistScore(Artist artist, int score) {
        if (artist.getPopularityScore() < score) {
            artist.setPopularityScore(score);
            artistRepository.save(artist);
            LOGGER.info("Updated artist popularity score: {} -> {}", artist.getName(), score);
        }
    }

    private void seedTopDirectorsFromDB() {
        try {
            List<Director> topDirectors = directorRepository.findAll().stream()
                .sorted((d1, d2) -> {
                    int count1 = d1.getEvents() != null ? d1.getEvents().size() : 0;
                    int count2 = d2.getEvents() != null ? d2.getEvents().size() : 0;
                    return Integer.compare(count2, count1);
                })
                .limit(30)
                .toList();
            for (Director director : topDirectors) {
                updateDirectorScore(director, MEDIUM_PRIORITY_SCORE);
            }
        } catch (Exception e) {
            LOGGER.error("Error seeding directors from DB", e);
        }
    }

    private void seedTopActorsFromDB() {
        try {
            List<Actor> topActors = actorRepository.findAll().stream()
                .sorted((a1, a2) -> {
                    int count1 = a1.getEvents() != null ? a1.getEvents().size() : 0;
                    int count2 = a2.getEvents() != null ? a2.getEvents().size() : 0;
                    return Integer.compare(count2, count1);
                })
                .limit(50)
                .toList();
            for (Actor actor : topActors) {
                updateActorScore(actor, MEDIUM_PRIORITY_SCORE);
            }
        } catch (Exception e) {
            LOGGER.error("Error seeding actors from DB", e);
        }
    }

    private void updateDirectorScore(Director director, int score) {
        if (director.getPopularityScore() < score) {
            director.setPopularityScore(score);
            directorRepository.save(director);
            LOGGER.info("Updated director popularity score: {} -> {}", director.getName(), score);
        }
    }

    private void updateActorScore(Actor actor, int score) {
        if (actor.getPopularityScore() < score) {
            actor.setPopularityScore(score);
            actorRepository.save(actor);
            LOGGER.info("Updated actor popularity score: {} -> {}", actor.getName(), score);
        }
    }

    private static class PopularityData {
        private List<String> topVenues;
        private List<String> topArtists;

        public List<String> getTopVenues() { return topVenues; }
        public void setTopVenues(List<String> topVenues) { this.topVenues = topVenues; }
        public List<String> getTopArtists() { return topArtists; }
        public void setTopArtists(List<String> topArtists) { this.topArtists = topArtists; }
    }
}
