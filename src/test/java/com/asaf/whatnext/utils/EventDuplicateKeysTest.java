package com.asaf.whatnext.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventDuplicateKeysTest {

    @Test
    void sameKey_forFoodieFestCasingAndPunctuation() {
        LocalDate d = LocalDate.of(2026, 5, 2);
        String a = "Yüzdeyüz Müzik Sunar: FoodieFest 4";
        String b = "Yüzdeyüz Müzik Sunar: Foodiefest 4";
        assertEquals(EventDuplicateKeys.titleAndDateKey(a, d), EventDuplicateKeys.titleAndDateKey(b, d));
    }
}
