package com.asaf.whatnext.enums.Biletix;

public enum BiletixCity {
    ISTANBUL("istanbul"),
    ANKARA("ankara"),
    IZMIR("izmir");

    private final String value;

    BiletixCity(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
} 