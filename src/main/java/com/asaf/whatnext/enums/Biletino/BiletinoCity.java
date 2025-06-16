package com.asaf.whatnext.enums.Biletino;

public enum BiletinoCity {
    ISTANBUL("istanbul"),
    ANKARA("ankara"),
    IZMIR("izmir");

    private final String value;

    BiletinoCity(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
} 