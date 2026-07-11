package com.asaf.whatnext.scraper.bubilet;

import com.asaf.whatnext.scraper.common.DateScrapeStrategy;

import java.util.regex.Pattern;

public final class BubiletDateRules {

    /**
     * Baştan bağlantılı: "17 Temmuz Cum 21:00", "08 Mayıs, 09 Mayıs" gibi liste tarih satırları eşleşir;
     * "Mayıs Sahnesi" gibi ay adı İÇEREN ama tarihle BAŞLAMAYAN mekân adları eşleşmez (eski {@code .*ay.*} bug'ı).
     */
    private static final Pattern TURKISH_MONTH = Pattern.compile(
            "^\\d{1,2}\\s+(Ocak|Şubat|Mart|Nisan|Mayıs|Haziran|Temmuz|Ağustos|Eylül|Ekim|Kasım|Aralık)"
                    + "(\\s+\\S+)?(\\s+\\d{1,2}[:.]\\d{2})?.*");

    private BubiletDateRules() {
    }

    public static boolean looksLikeListDateLine(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        if (text.contains("₺") || text.contains("Yakında") || text.contains("Satışta") || text.contains("Tükendi")) {
            return false;
        }
        return TURKISH_MONTH.matcher(text.trim()).matches();
    }

    /**
     * {@code /mekan/} anchor'ı yoksa ve satır tarih desenine uymuyorsa: virgülle ayrılmış mekân listesi
     * ("Hilltown Seyirlik Sahne, Trump Sahne, Kadıköy Eğitim Sahnesi") — çok-mekânlı tuzak.
     */
    public static boolean looksLikeVenueList(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return !looksLikeListDateLine(text);
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
