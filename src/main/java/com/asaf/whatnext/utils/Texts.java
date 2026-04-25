package com.asaf.whatnext.utils;

/**
 * Scraper / ham metin ayrıştırmada kullanılan küçük yardımcılar.
 */
public final class Texts {

    private Texts() {
    }

    /**
     * İlk anlamlı (null değil, sadece boşluk değil) metni döndürür; yoksa boş string.
     */
    public static String firstNonBlank(String... parts) {
        if (parts == null) {
            return "";
        }
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                return p.trim();
            }
        }
        return "";
    }
}
