package com.asaf.whatnext.scraper.common;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ElementTextExtractorTest {

    private static final String[] SELECTORS = {".a", ".b", ".c"};

    @Test
    void skipsBlanksAndReturnsFirstText() {
        var texts = Map.of(".a", "  ", ".b", "  Cemal Reşit Rey  ");
        String v = ElementTextExtractor.firstNonBlank(
                s -> texts.getOrDefault(s, ""), SELECTORS);
        assertThat(v).isEqualTo("Cemal Reşit Rey");
    }

    @Test
    void allBlank_returnsEmpty() {
        String v = ElementTextExtractor.firstNonBlank(s -> " \t", SELECTORS);
        assertThat(v).isEmpty();
    }
}
