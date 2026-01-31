package com.asaf.whatnext.enums.Bubilet;

public enum BubiletCity {
    ISTANBUL("istanbul"),
    ANKARA("ankara"),
    IZMIR("izmir");

    private final String value;

    BubiletCity(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
