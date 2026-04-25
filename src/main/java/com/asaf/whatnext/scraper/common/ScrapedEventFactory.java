package com.asaf.whatnext.scraper.common;

import com.asaf.whatnext.enums.EventSourceType;
import com.asaf.whatnext.scraper.ScrapedEventData;

/**
 * Sık kullanılan {@link ScrapedEventData#builder()} kalıpları.
 */
public final class ScrapedEventFactory {

    private ScrapedEventFactory() {
    }

    public static ScrapedEventData.ScrapedEventDataBuilder baseFor(EventSourceType source) {
        return ScrapedEventData.builder().source(source);
    }
}
