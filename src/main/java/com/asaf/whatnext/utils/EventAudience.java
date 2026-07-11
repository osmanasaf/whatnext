package com.asaf.whatnext.utils;

import com.asaf.whatnext.enums.AudienceType;
import com.asaf.whatnext.models.Event;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Görsel/popüler etkinlik listelerinde hedef kitleye göre dışlama kuralları.
 * Ground truth: Bubilet {@code cocuk-aktiviteleri} etiketinden gelen {@link Event#getAudience()}.
 * O alan yoksa (diğer kaynaklar, eski satırlar) {@link #classify} sezgiseline düşülür.
 */
public final class EventAudience {

    private EventAudience() {
    }

    /**
     * Sadece küçük yaşlar (0-12) + "+" işaretiyle ya da "yaş grubu" ibaresiyle eşleşir —
     * çıplak "25 Yaş Günü Partisi" gibi yetişkin bağlamlarını yanlış pozitif yapmamak için daraltıldı.
     */
    private static final Pattern AGE_PATTERN = Pattern.compile(
            "\\b([0-9]|1[0-2])\\s*\\+\\s*yas\\b"
                    + "|\\byas\\s+grubu\\b"
                    + "|\\b([0-9]|1[0-2])\\s*-\\s*([0-9]|1[0-2])\\s*yas\\b");

    private static final List<String> KEYWORDS = List.of(
            "cocuk", "bebek", "minik", "masal", "kukla");

    /** Bubilet {@code cocuk-aktiviteleri} etiketinde görülen bilinen çocuk oyunu/gösteri adları (statik seed). */
    private static final List<String> KNOWN_KIDS_TITLES = List.of(
            "pinokyo", "rapunzel", "karlar ulkesi", "kaptan pengu",
            "kirmizi baslikli kiz", "pamuk prenses");

    public static AudienceType classify(String title, String venueName) {
        String t = normalize(title);
        String v = normalize(venueName);

        if (AGE_PATTERN.matcher(t).find() || AGE_PATTERN.matcher(v).find()) {
            return AudienceType.CHILDREN;
        }
        for (String kw : KEYWORDS) {
            if (t.contains(kw) || v.contains(kw)) {
                return AudienceType.CHILDREN;
            }
        }
        for (String known : KNOWN_KIDS_TITLES) {
            if (t.contains(known)) {
                return AudienceType.CHILDREN;
            }
        }
        return AudienceType.UNKNOWN;
    }

    public static boolean isChildrensEvent(Event event) {
        if (event == null) {
            return false;
        }
        AudienceType audience = event.getAudience();
        if (audience == AudienceType.CHILDREN) {
            return true;
        }
        if (audience == AudienceType.GENERAL) {
            return false;
        }
        String venueName = event.getVenue() != null ? event.getVenue().getName() : null;
        return classify(event.getTitle(), venueName) == AudienceType.CHILDREN;
    }

    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.toLowerCase(Locale.forLanguageTag("tr"))
                .replace('ı', 'i').replace('İ', 'i')
                .replace('ğ', 'g')
                .replace('ü', 'u')
                .replace('ş', 's')
                .replace('ö', 'o')
                .replace('ç', 'c');
    }
}
