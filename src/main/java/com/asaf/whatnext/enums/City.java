package com.asaf.whatnext.enums;

public enum City {
    ISTANBUL("Istanbul"),
    IZMIR("Izmir"),
    ANKARA("Ankara"),
    ANTALYA("Antalya");

    private final String displayName;

    City(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 