package com.asaf.whatnext.enums.Biletix;

public enum BiletixCategory {
    MUSIC("MUSIC"),
    SPORT("SPORT"),
    ART("ART");

    private final String value;
    BiletixCategory(String value) { this.value = value; }
    public String getValue() { return value; }
} 