package com.asaf.whatnext.utils;

import com.asaf.whatnext.models.Event;

import java.util.Locale;

/**
 * Görsel/popüler etkinlik listelerinde hedef kitleye göre basit dışlama kuralları.
 */
public final class EventAudience {

    private EventAudience() {
    }

    public static boolean isChildrensEvent(Event event) {
        if (event == null) {
            return false;
        }

        String title = normalize(event.getTitle());
        if (title.contains("çocuk") || title.contains("cocuk") || title.contains("yaş grubu")) {
            return true;
        }

        if (event.getVenue() != null && event.getVenue().getName() != null) {
            String venueName = normalize(event.getVenue().getName());
            return venueName.contains("çocuk") || venueName.contains("cocuk");
        }

        return false;
    }

    private static String normalize(String s) {
        return s == null ? "" : s.toLowerCase(Locale.forLanguageTag("tr"));
    }
}
