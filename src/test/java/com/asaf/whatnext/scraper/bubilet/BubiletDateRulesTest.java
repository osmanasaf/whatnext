package com.asaf.whatnext.scraper.bubilet;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BubiletDateRulesTest {

    @Test
    void looksLikeListDateLine_falseForPriceOrSales() {
        assertThat(BubiletDateRules.looksLikeListDateLine("150₺ Satışta")).isFalse();
        assertThat(BubiletDateRules.looksLikeListDateLine("Yakında")).isFalse();
    }

    @Test
    void looksLikeListDateLine_trueForTurkishMonth() {
        assertThat(BubiletDateRules.looksLikeListDateLine("15 Nisan 2025")).isTrue();
    }

    @Test
    void requiresDetailPage_trueWhenCommaAndNoDashRange() {
        assertThat(BubiletDateRules.requiresDetailPage("12 Şub, 14 Şub")).isTrue();
    }

    @Test
    void requiresDetailPage_falseWhenDashRange() {
        assertThat(BubiletDateRules.requiresDetailPage("12 Şub 2025 - 14 Şub 2025")).isFalse();
    }

    /** "Mayıs Sahnesi" ay adı İÇERİYOR ama tarihle BAŞLAMIYOR — eski {@code .*ay.*} regex bug'ının regresyonu. */
    @Test
    void looksLikeListDateLine_falseForVenueNameContainingMonthWord() {
        assertThat(BubiletDateRules.looksLikeListDateLine("Mayıs Sahnesi")).isFalse();
        assertThat(BubiletDateRules.looksLikeListDateLine("Nisan Kültür Merkezi")).isFalse();
    }

    @Test
    void looksLikeListDateLine_trueForFullListDateLine() {
        assertThat(BubiletDateRules.looksLikeListDateLine("17 Temmuz Cum 21:00")).isTrue();
        assertThat(BubiletDateRules.looksLikeListDateLine("08 Mayıs, 09 Mayıs")).isTrue();
    }

    @Test
    void looksLikeVenueList_trueForCommaSeparatedVenueNames() {
        assertThat(BubiletDateRules.looksLikeVenueList("Hilltown Seyirlik Sahne, Trump Sahne, Kadıköy Eğitim Sahnesi")).isTrue();
    }

    @Test
    void looksLikeVenueList_falseForActualDateLine() {
        assertThat(BubiletDateRules.looksLikeVenueList("17 Temmuz Cum 21:00")).isFalse();
    }
}
