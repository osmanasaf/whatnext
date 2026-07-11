package com.asaf.whatnext.enums.Bubilet;

public enum BubiletCategory {
    THEATRE("tiyatro"),
    MUSIC("konser"),
    STANDUP("stand-up"),
    /** Ground truth: {@code cocuk-aktiviteleri} etiketi — çocuk etkinliği tespiti için (bkz. Faz 2). */
    KIDS("cocuk-aktiviteleri");

    private final String value;

    BubiletCategory(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
