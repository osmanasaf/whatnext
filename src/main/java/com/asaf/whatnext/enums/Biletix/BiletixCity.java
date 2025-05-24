package com.asaf.whatnext.enums.Biletix;

public enum BiletixCity {
    ISTANBUL("%C4%B0stanbul"); // URL encoded

    private final String value;
    BiletixCity(String value) { this.value = value; }
    public String getValue() { return value; }
} 