package com.asaf.whatnext.services.scoring;

import com.asaf.whatnext.models.Actor;
import com.asaf.whatnext.models.ConcertEvent;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.models.PerformingArt;
import com.asaf.whatnext.services.TitlePopularityService;
import org.springframework.stereotype.Service;

@Service
public class DefaultEventScoringStrategy extends AbstractEventScoringStrategy {

    public DefaultEventScoringStrategy(TitlePopularityService titlePopularityService) {
        super(titlePopularityService);
    }

    @Override
    protected int calculateEventTypeSpecificScore(Event event) {
        int score = 0;

        if (event instanceof ConcertEvent concert) {
            if (concert.getArtist() != null) {
                score += concert.getArtist().getPopularityScore();
            }
        }

        if (event instanceof PerformingArt performingArt) {
            if (performingArt.getDirector() != null) {
                score += performingArt.getDirector().getPopularityScore();
            }
            
            if (performingArt.getActors() != null && !performingArt.getActors().isEmpty()) {
                int actorScore = performingArt.getActors().stream()
                    .mapToInt(Actor::getPopularityScore)
                    .sorted()
                    .skip(Math.max(0, performingArt.getActors().size() - 2))
                    .sum();
                score += actorScore;
            }
        }

        return score;
    }
}
