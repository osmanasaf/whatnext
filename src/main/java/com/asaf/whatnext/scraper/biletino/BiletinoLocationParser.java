package com.asaf.whatnext.scraper.biletino;

import com.asaf.whatnext.enums.Biletino.BiletinoCity;

import java.util.Locale;

/**
 * "Mekân, şehir" ayrıştırır. Virgül yoksa tüm metin mekân sayılır, şehir arama bağlamındaki şehirden gelir.
 */
public final class BiletinoLocationParser {

    private BiletinoLocationParser() {
    }

    public record VenueCity(String venueName, String city) {
    }

    public static VenueCity parse(String fullLocation, BiletinoCity searchCity) {
        String venueName = "";
        String city = searchCity != null ? searchCity.getValue() : "";

        if (fullLocation != null && !fullLocation.isEmpty()) {
            if (fullLocation.contains(",")) {
                String[] parts = fullLocation.split(",");
                venueName = parts[0].trim();
                if (parts.length > 1) {
                    city = parts[parts.length - 1].trim();
                }
            } else {
                venueName = fullLocation.trim();
                // Virgülsüz "Online Event" gibi metinlerde fiziksel şehir yok; arama şehrine düşürmek
                // canlı doğrulamada sanal etkinlikleri yanlışlıkla İstanbul'a etiketliyordu.
                if (fullLocation.toLowerCase(Locale.ROOT).contains("online")) {
                    city = "";
                }
            }
        }

        return new VenueCity(venueName, city);
    }
}
