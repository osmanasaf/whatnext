package com.asaf.whatnext.repository;

import com.asaf.whatnext.models.FeaturedEvent;

import java.util.List;

public interface FeaturedEventRepository extends BaseRepository<FeaturedEvent, Long> {
    List<FeaturedEvent> findByEventType(String eventType);
}
