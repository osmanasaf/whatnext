package com.asaf.whatnext.services.scoring;

import com.asaf.whatnext.models.Event;

public interface EventScoringStrategy {
    int calculateScore(Event event);
}

