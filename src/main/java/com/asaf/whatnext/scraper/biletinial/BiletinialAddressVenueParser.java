package com.asaf.whatnext.scraper.biletinial;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Önce {@code small}; yok veya boşsa adres satırından (kalın şehir adı düşülerek) mekan metni.
 */
public final class BiletinialAddressVenueParser {

    private BiletinialAddressVenueParser() {
    }

    public static String extractVenueNameFromAddressHtml(String addressOuterHtml) {
        if (addressOuterHtml == null || addressOuterHtml.isBlank()) {
            return "";
        }
        Document doc = Jsoup.parse(addressOuterHtml);
        Element address = doc.selectFirst("address");
        if (address == null) {
            return "";
        }
        Element small = address.selectFirst("small");
        if (small != null) {
            String t = small.text();
            if (t != null) {
                t = t.trim();
                if (!t.isEmpty()) {
                    return t;
                }
            }
        }
        Element bold = address.selectFirst("b");
        if (bold != null) {
            String cityText = bold.text();
            String all = address.text();
            if (all != null && cityText != null) {
                String v = all.replace(cityText, "").replace("\n", " ");
                v = v.replaceAll("\\s+", " ").trim();
                if (!v.isEmpty()) {
                    return v;
                }
            }
        }
        return "";
    }
}
