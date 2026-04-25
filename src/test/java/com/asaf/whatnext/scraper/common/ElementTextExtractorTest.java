package com.asaf.whatnext.scraper.common;

import com.asaf.whatnext.config.BiletixScraperConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ElementTextExtractorTest {

    @Test
    void skipsBlanksAndReturnsFirstText() {
        var texts = Map.of(
                BiletixScraperConfig.EVENT_VENUE_SELECTOR, "  ",
                ".searchResultEventPlace", "  Cemal Reşit Rey  ");
        String v = ElementTextExtractor.firstNonBlank(
                s -> texts.getOrDefault(s, ""),
                BiletixScraperConfig.EVENT_VENUE_ALTERNATE_SELECTORS);
        assertThat(v).isEqualTo("Cemal Reşit Rey");
    }

    @Test
    void allBlank_returnsEmpty() {
        String v = ElementTextExtractor.firstNonBlank(
                s -> " \t",
                BiletixScraperConfig.EVENT_VENUE_ALTERNATE_SELECTORS);
        assertThat(v).isEmpty();
    }
}
