package com.asaf.whatnext.scraper.common;

import org.jsoup.nodes.Element;

import java.util.function.Function;

/**
 * Çoklu CSS seçici ile (Jsoup veya WebDriver üzerinde) ilk dolu metin.
 */
public final class ElementTextExtractor {

    private ElementTextExtractor() {
    }

    public static String firstNonBlank(Function<String, String> textBySelector, String... selectors) {
        for (String sel : selectors) {
            String t = textBySelector.apply(sel);
            if (t != null && !t.isBlank()) {
                return t.trim();
            }
        }
        return "";
    }

    public static String firstNonBlankFromChildren(Element root, String... cssSelectors) {
        if (root == null) {
            return "";
        }
        for (String sel : cssSelectors) {
            String value = text(root.selectFirst(sel));
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String text(Element element) {
        return element == null ? "" : element.text().trim();
    }
}
