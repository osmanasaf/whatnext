package com.asaf.whatnext.utils;

import com.asaf.whatnext.enums.AudienceType;
import com.asaf.whatnext.models.Event;
import com.asaf.whatnext.models.Venue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventAudienceTest {

    @Test
    void classify_knownKidsTitles_areChildren() {
        assertThat(EventAudience.classify("Pinokyo Tiyatro Oyunu", null)).isEqualTo(AudienceType.CHILDREN);
        assertThat(EventAudience.classify("Karlar Ülkesi", null)).isEqualTo(AudienceType.CHILDREN);
        assertThat(EventAudience.classify("Rapunzel Çocuk Oyunu", null)).isEqualTo(AudienceType.CHILDREN);
        assertThat(EventAudience.classify("Kaptan Pengu Gösterisi", null)).isEqualTo(AudienceType.CHILDREN);
    }

    @Test
    void classify_agePatterns_areChildren() {
        assertThat(EventAudience.classify("Masal Diyarı 3+ Yaş", null)).isEqualTo(AudienceType.CHILDREN);
        assertThat(EventAudience.classify("Bebek Konseri 0-3 Yaş Grubu", null)).isEqualTo(AudienceType.CHILDREN);
    }

    @Test
    void classify_childVenueName_isChildren() {
        assertThat(EventAudience.classify("Gösteri", "Küçük Prens Çocuk Sahnesi")).isEqualTo(AudienceType.CHILDREN);
    }

    @Test
    void classify_adultEvents_areUnknown() {
        assertThat(EventAudience.classify("Cem Yılmaz Stand-Up Gösterisi", "Zorlu PSM")).isEqualTo(AudienceType.UNKNOWN);
        assertThat(EventAudience.classify("Sezen Aksu Konseri", "Volkswagen Arena")).isEqualTo(AudienceType.UNKNOWN);
    }

    /** Daraltılmış yaş regex'inin asıl amacı: çıplak "25 Yaş Günü" gibi yetişkin bağlamı yanlış pozitif olmamalı. */
    @Test
    void classify_adultAgeMention_isNotFalsePositive() {
        assertThat(EventAudience.classify("25. Yaş Günü Partisi", null)).isEqualTo(AudienceType.UNKNOWN);
    }

    @Test
    void isChildrensEvent_audienceChildren_true() {
        Event e = new Event();
        e.setAudience(AudienceType.CHILDREN);
        e.setTitle("Herhangi Bir Başlık");
        assertThat(EventAudience.isChildrensEvent(e)).isTrue();
    }

    @Test
    void isChildrensEvent_audienceGeneral_overridesHeuristic() {
        Event e = new Event();
        e.setAudience(AudienceType.GENERAL);
        e.setTitle("Pinokyo Tiyatro Oyunu");
        assertThat(EventAudience.isChildrensEvent(e)).isFalse();
    }

    @Test
    void isChildrensEvent_audienceNull_fallsBackToClassify() {
        Event e = new Event();
        e.setTitle("Karlar Ülkesi");
        assertThat(EventAudience.isChildrensEvent(e)).isTrue();
    }

    @Test
    void isChildrensEvent_audienceUnknown_fallsBackToClassify_negativeCase() {
        Event e = new Event();
        e.setAudience(AudienceType.UNKNOWN);
        e.setTitle("Cem Yılmaz Stand-Up Gösterisi");
        Venue venue = new Venue();
        venue.setName("Zorlu PSM");
        e.setVenue(venue);
        assertThat(EventAudience.isChildrensEvent(e)).isFalse();
    }
}
