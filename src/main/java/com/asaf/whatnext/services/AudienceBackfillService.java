package com.asaf.whatnext.services;

import com.asaf.whatnext.enums.AudienceType;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.repository.EventRepository;
import com.asaf.whatnext.utils.EventAudience;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Eski/diğer-kaynak satırlarının {@code audience} alanını sezgiselle ({@link EventAudience#classify})
 * geri doldurur — {@code POST /api/events/audience/backfill?dryRun=true}.
 */
@Service
public class AudienceBackfillService {

    private final EventRepository eventRepository;

    @Autowired
    public AudienceBackfillService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional
    public Map<String, Object> backfill(boolean dryRun) {
        List<Event> candidates = eventRepository.findAll().stream()
                .filter(e -> e.getAudience() == null || e.getAudience() == AudienceType.UNKNOWN)
                .toList();

        List<Map<String, Object>> changes = new ArrayList<>();
        for (Event e : candidates) {
            String venueName = e.getVenue() != null ? e.getVenue().getName() : null;
            AudienceType classified = EventAudience.classify(e.getTitle(), venueName);
            if (classified != AudienceType.CHILDREN) {
                continue;
            }
            changes.add(Map.of("id", e.getId(), "title", e.getTitle(), "newAudience", classified));
            if (!dryRun) {
                // @Transactional altında yönetilen entity — Hibernate dirty-checking commit'te flush eder.
                e.setAudience(classified);
            }
        }

        return Map.of(
                "dryRun", dryRun,
                "scanned", candidates.size(),
                "matched", changes.size(),
                "changes", changes);
    }
}
