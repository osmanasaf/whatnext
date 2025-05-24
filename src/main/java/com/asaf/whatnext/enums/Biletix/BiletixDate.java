package com.asaf.whatnext.enums.Biletix;

public enum BiletixDate {
    TODAY("today"),
    THISWEEK("thisweek");

    private final String value;
    BiletixDate(String value) { this.value = value; }
    public String getValue() { return value; }
} 