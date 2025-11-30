package com.asaf.whatnext.controllers;

import com.asaf.whatnext.dto.WeeklyPopularEventsDTO;
import com.asaf.whatnext.services.PopularEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events/popular")
public class PopularEventController {

    private final PopularEventService popularEventService;

    public PopularEventController(PopularEventService popularEventService) {
        this.popularEventService = popularEventService;
    }

    @GetMapping("/weekly")
    public ResponseEntity<WeeklyPopularEventsDTO> getWeeklyPopularEvents() {
        return ResponseEntity.ok(popularEventService.getWeeklyPopularEvents());
    }
}

