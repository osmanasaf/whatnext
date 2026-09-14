package com.asaf.whatnext.services.scoring;

import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.services.TitlePopularityService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Skor = başlık tekrarı (0–15) + mekân (0–10) + sanatçı/oyun/komedyen (0–10+) + kaynak popülerliği (0–20)
 * + tarih yakınlığı (0–10). Kaynak popülerliği tek gerçek talep sinyali (Biletix çok satanlar), bu yüzden
 * en ağır bileşen; diğerleri elle seçilmiş liste veya sıklık tahmini.
 */
public abstract class AbstractEventScoringStrategy implements EventScoringStrategy {

    protected static final int DATE_PROXIMITY_BONUS_MAX = 10;
    protected static final int DAYS_FOR_MAX_BONUS = 3;
    protected static final int DATE_BONUS_MEDIUM_RANGE = 7;

    protected final TitlePopularityService titlePopularityService;
    protected final PopularityCatalog popularityCatalog;

    protected AbstractEventScoringStrategy(TitlePopularityService titlePopularityService, PopularityCatalog popularityCatalog) {
        this.titlePopularityService = titlePopularityService;
        this.popularityCatalog = popularityCatalog;
    }

    @Override
    public final int calculateScore(Event event) {
        int score = 0;

        score += calculateTitleScore(event);
        score += calculateVenueScore(event);
        score += calculateEventTypeSpecificScore(event);
        score += calculateSourcePopularityScore(event);
        score += calculateDateProximityBonus(event);

        return score;
    }

    protected int calculateTitleScore(Event event) {
        if (event.getTitle() == null || event.getType() == null) {
            return 0;
        }
        return titlePopularityService.calculateTitlePopularityScore(
            event.getTitle(),
            event.getType()
        );
    }

    /** DB puanı (etkinlik sayısından) ile elle seçilmiş listeden büyüğü — ikisi toplanmaz. */
    protected int calculateVenueScore(Event event) {
        if (event.getVenue() != null) {
            return Math.max(event.getVenue().getPopularityScore(),
                    popularityCatalog.venueScore(event.getVenue().getName()));
        }
        return 0;
    }

    protected int calculateSourcePopularityScore(Event event) {
        return event.getSourcePopularity() != null ? event.getSourcePopularity() : 0;
    }

    protected abstract int calculateEventTypeSpecificScore(Event event);

    protected int calculateDateProximityBonus(Event event) {
        if (event.getStartDate() == null) {
            return 0;
        }

        long daysUntilEvent = ChronoUnit.DAYS.between(LocalDate.now(), event.getStartDate());

        if (daysUntilEvent >= 0 && daysUntilEvent <= DAYS_FOR_MAX_BONUS) {
            return DATE_PROXIMITY_BONUS_MAX - (int) (daysUntilEvent * (DATE_PROXIMITY_BONUS_MAX / DAYS_FOR_MAX_BONUS));
        } else if (daysUntilEvent > DAYS_FOR_MAX_BONUS && daysUntilEvent <= DATE_BONUS_MEDIUM_RANGE) {
            return 2;
        }

        return 0;
    }
}
