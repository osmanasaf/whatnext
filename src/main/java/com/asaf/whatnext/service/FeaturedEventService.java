package com.asaf.whatnext.service;

import com.asaf.whatnext.models.FeaturedEvent;
import com.asaf.whatnext.repository.FeaturedEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeaturedEventService extends BaseService<FeaturedEvent, Long> {

    public FeaturedEventService(FeaturedEventRepository repository) {
        super(repository);
    }

    public List<FeaturedEvent> findByEventType(String eventType) {
        return ((FeaturedEventRepository) repository).findByEventType(eventType);
    }
}
