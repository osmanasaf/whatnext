package com.asaf.whatnext.repository;

import com.asaf.whatnext.models.TheaterEvent;

import java.util.List;

public interface TheaterEventRepository extends BaseRepository<TheaterEvent, Long> {
    List<TheaterEvent> findByPlaywright(String playwright);
    List<TheaterEvent> findByActorsId(Long actorId);
}
