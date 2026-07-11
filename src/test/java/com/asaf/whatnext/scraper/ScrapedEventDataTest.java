package com.asaf.whatnext.scraper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScrapedEventDataTest {

    @Test
    void normalizeCity_stripsCombiningDot_nfdIstanbulFromBiletix() {
        // Biletix: "i" (U+0069) + COMBINING DOT ABOVE (U+0307) + "stanbul"
        String nfd = "i\u0307stanbul";
        assertEquals("istanbul", ScrapedEventData.normalizeCity(nfd));
    }

    @Test
    void normalizeCity_plainIstanbul() {
        assertEquals("istanbul", ScrapedEventData.normalizeCity("İstanbul"));
        assertEquals("istanbul", ScrapedEventData.normalizeCity("istanbul"));
    }

    @Test
    void normalizeCity_izmirNfdLikeBiletix() {
        assertEquals("izmir", ScrapedEventData.normalizeCity("i\u0307zmir"));
        assertEquals("izmir", ScrapedEventData.normalizeCity("İzmir"));
    }
}
