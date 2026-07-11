package com.asaf.whatnext.services;

import com.asaf.whatnext.enums.AudienceType;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.repository.EventRepository;
import com.asaf.whatnext.utils.EventIdentity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mevcut veri temizliği (Faz 3.3) — {@code POST /api/events/dedup?dryRun=true}. Tüm etkinlikler
 * {@code startDate}'e göre gruplanır, gün içi ikili {@link EventIdentity} taraması yapılır;
 * gerçek modda ({@code dryRun=false}) en düşük id kalır, ticketUrl'ler ona taşınır, diğerleri silinir.
 */
@Service
public class EventDedupService {

    private final EventRepository eventRepository;
    private final double jaccardThreshold;
    private final double jaccardStrongThreshold;

    @Autowired
    public EventDedupService(
            EventRepository eventRepository,
            @Value("${whatnext.dedup.title-jaccard:0.5}") double jaccardThreshold,
            @Value("${whatnext.dedup.title-jaccard-strong:0.85}") double jaccardStrongThreshold) {
        this.eventRepository = eventRepository;
        this.jaccardThreshold = jaccardThreshold;
        this.jaccardStrongThreshold = jaccardStrongThreshold;
    }

    @Transactional
    public Map<String, Object> dedup(boolean dryRun) {
        Map<LocalDate, List<Event>> byDate = new LinkedHashMap<>();
        for (Event e : eventRepository.findAll()) {
            if (e.getStartDate() != null) {
                byDate.computeIfAbsent(e.getStartDate(), d -> new ArrayList<>()).add(e);
            }
        }

        List<Map<String, Object>> groups = new ArrayList<>();
        int eventsMerged = 0;
        for (List<Event> dayEvents : byDate.values()) {
            for (List<Event> cluster : clusterByIdentity(dayEvents)) {
                if (cluster.size() < 2) {
                    continue;
                }
                cluster.sort(Comparator.comparing(Event::getId));
                Event keeper = cluster.get(0);
                List<Event> duplicates = cluster.subList(1, cluster.size());

                groups.add(Map.of(
                        "keptId", keeper.getId(),
                        "keptTitle", keeper.getTitle(),
                        "startDate", keeper.getStartDate().toString(),
                        "removedIds", duplicates.stream().map(Event::getId).toList(),
                        "removedTitles", duplicates.stream().map(Event::getTitle).toList()));

                if (!dryRun) {
                    for (Event dup : duplicates) {
                        mergeInto(keeper, dup);
                    }
                    eventRepository.save(keeper);
                    eventRepository.deleteAllById(duplicates.stream().map(Event::getId).toList());
                }
                eventsMerged += duplicates.size();
            }
        }

        return Map.of(
                "dryRun", dryRun,
                "groupsFound", groups.size(),
                "eventsMerged", eventsMerged,
                "groups", groups);
    }

    private void mergeInto(Event keeper, Event duplicate) {
        duplicate.getTicketUrls().forEach(keeper::addTicketUrl);
        if (keeper.getVenue() == null && duplicate.getVenue() != null) {
            keeper.setVenue(duplicate.getVenue());
        }
        if (keeper.getMinPriceTl() == null
                || (duplicate.getMinPriceTl() != null && duplicate.getMinPriceTl() < keeper.getMinPriceTl())) {
            keeper.setMinPriceTl(duplicate.getMinPriceTl());
        }
        if (duplicate.getAudience() == AudienceType.CHILDREN) {
            keeper.setAudience(AudienceType.CHILDREN);
        }
    }

    /** Greedy tek-geçiş kümeleme: her etkinlik, kendisiyle eşleşen ilk kümenin temsilcisiyle karşılaştırılır. */
    private List<List<Event>> clusterByIdentity(List<Event> events) {
        List<List<Event>> clusters = new ArrayList<>();
        for (Event e : events) {
            List<Event> matchedCluster = null;
            for (List<Event> cluster : clusters) {
                if (EventIdentity.isSameEvent(cluster.get(0), e, jaccardThreshold, jaccardStrongThreshold)) {
                    matchedCluster = cluster;
                    break;
                }
            }
            if (matchedCluster != null) {
                matchedCluster.add(e);
            } else {
                List<Event> newCluster = new ArrayList<>();
                newCluster.add(e);
                clusters.add(newCluster);
            }
        }
        return clusters;
    }
}
