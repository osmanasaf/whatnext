package com.asaf.whatnext.utils;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Scraper'larda kullanılan "bugün" — sistem saat dilimi yerine daima Europe/Istanbul.
 * Sunucu farklı bir saat diliminde çalışırsa yıl-devri/rollover hesapları kayardı.
 */
public final class TrClock {

    public static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");

    private TrClock() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
