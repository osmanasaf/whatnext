package com.asaf.whatnext.scraper.bubilet;

import com.asaf.whatnext.scraper.common.DateScrapeStrategy;

import java.util.regex.Pattern;

public final class BubiletDateRules {

    private static final Pattern TURKISH_MONTH = Pattern.compile(
            ".*(Ocak|Şubat|Mart|Nisan|Mayıs|Haziran|Temmuz|Ağustos|Eylül|Ekim|Kasım|Aralık).*");

    private BubiletDateRules() {
    }

    public static boolean looksLikeListDateLine(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        if (text.contains("₺") || text.contains("Yakında") || text.contains("Satışta") || text.contains("Tükendi")) {
            return false;
        }
        return TURKISH_MONTH.matcher(text).matches();
    }

    public static boolean requiresDetailPage(String dateText) {
        if (dateText == null || dateText.isBlank()) {
            return true;
        }
        String t = dateText.trim();
        if (t.contains("...")) {
            return true;
        }
        return t.contains(",") && !t.contains(" - ");
    }

    public static DateScrapeStrategy toStrategy(String dateText) {
        return requiresDetailPage(dateText) ? DateScrapeStrategy.REQUIRES_DETAIL_PAGE : DateScrapeStrategy.FROM_LIST_ONLY;
    }
}
