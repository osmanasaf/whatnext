package com.asaf.whatnext.enums.Biletinial;

public enum BiletinialCity {
    ISTANBUL("istanbul"),
    ANKARA("ankara"),
    IZMIR("izmir"),
    ANTALYA("antalya");

    private final String value;

    BiletinialCity(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
} 