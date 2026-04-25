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
}
