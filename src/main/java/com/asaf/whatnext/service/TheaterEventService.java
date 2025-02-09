package com.asaf.whatnext.service;

import com.asaf.whatnext.models.TheaterEvent;
import com.asaf.whatnext.repository.TheaterEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TheaterEventService extends BaseService<TheaterEvent, Long> {

    public TheaterEventService(TheaterEventRepository repository) {
        super(repository);
    }

    public List<TheaterEvent> findByPlaywright(String playwright) {
        return ((TheaterEventRepository) repository).findByPlaywright(playwright);
    }

    public List<TheaterEvent> findByActor(Long actorId) {
        return ((TheaterEventRepository) repository).findByActorsId(actorId);
    }
}
