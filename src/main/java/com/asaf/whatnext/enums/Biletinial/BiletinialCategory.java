package com.asaf.whatnext.enums.Biletinial;

public enum BiletinialCategory {
    THEATRE("tiyatro"),
    MUSIC("muzik"),
    STANDUP("stand-up"),
    EXHIBITION("sergi");

    private final String value;

    BiletinialCategory(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
} 