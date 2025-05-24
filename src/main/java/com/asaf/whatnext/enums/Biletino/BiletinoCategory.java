package com.asaf.whatnext.enums.Biletino;

public enum BiletinoCategory {
    MUSIC("100"),
    THEATRE("300"),
    COMEDY("1000");

    private final String value;
    BiletinoCategory(String value) { this.value = value; }
    public String getValue() { return value; }
}