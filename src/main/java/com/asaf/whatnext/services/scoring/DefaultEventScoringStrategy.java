package com.asaf.whatnext.services.scoring;

import com.asaf.whatnext.models.ConcertEvent;
import com.asaf.whatnext.models.Event;
import org.springframework.stereotype.Service;

@Service
public class DefaultEventScoringStrategy implements EventScoringStrategy {

    @Override
    public int calculateScore(Event event) {
        int score = 0;

        if (event.getVenue() != null) {
            score += event.getVenue().getPopularityScore();
        }

        if (event instanceof ConcertEvent) {
            ConcertEvent concert = (ConcertEvent) event;
            if (concert.getArtist() != null) {
                score += concert.getArtist().getPopularityScore();
            }
        }
        
        return score;
    }
}
