package com.asaf.whatnext.config;

import com.asaf.whatnext.models.Venue;
import com.asaf.whatnext.models.Artist;
import com.asaf.whatnext.models.Director;
import com.asaf.whatnext.models.Actor;
import com.asaf.whatnext.repository.VenueRepository;
import com.asaf.whatnext.repository.ArtistRepository;
import com.asaf.whatnext.repository.DirectorRepository;
import com.asaf.whatnext.repository.ActorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * DB'deki etkinlik sayısına göre "orta" popülerlik puanı (en çok etkinliği olan mekân/sanatçı/yönetmen/oyuncu).
 * {@code popular_config.json}'daki elle seçilmiş liste artık burada DB'ye yazılmıyor —
 * {@link com.asaf.whatnext.services.scoring.PopularityCatalog} onu puanlama anında her türe uyguluyor.
 */
@Component
public class PopularitySeeder implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PopularitySeeder.class);
    private static final int MEDIUM_PRIORITY_SCORE = 5;

    private final VenueRepository venueRepository;
    private final ArtistRepository artistRepository;
    private final DirectorRepository directorRepository;
    private final ActorRepository actorRepository;

    public PopularitySeeder(VenueRepository venueRepository, ArtistRepository artistRepository,
                           DirectorRepository directorRepository, ActorRepository actorRepository) {
        this.venueRepository = venueRepository;
        this.artistRepository = artistRepository;
        this.directorRepository = directorRepository;
        this.actorRepository = actorRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedTopVenuesFromDB();
        seedTopArtistsFromDB();
        seedTopDirectorsFromDB();
        seedTopActorsFromDB();
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
}
