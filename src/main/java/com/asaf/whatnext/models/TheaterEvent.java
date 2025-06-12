package com.asaf.whatnext.models;

import com.asaf.whatnext.enums.PerformanceType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
public class TheaterEvent extends Event {
    @Enumerated(EnumType.STRING)
    private PerformanceType performanceType;

    public PerformanceType getPerformanceType() {
        return performanceType;
    }

    public void setPerformanceType(PerformanceType performanceType) {
        this.performanceType = performanceType;
    }
} 