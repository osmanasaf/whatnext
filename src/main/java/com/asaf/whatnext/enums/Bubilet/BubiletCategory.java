package com.asaf.whatnext.enums.Bubilet;

public enum BubiletCategory {
    THEATRE("tiyatro"),
    MUSIC("konser"),
    STANDUP("stand-up");

    private final String value;

    BubiletCategory(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
