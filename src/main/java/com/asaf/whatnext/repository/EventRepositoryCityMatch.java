package com.asaf.whatnext.repository;

/**
 * Şehir alanı JPQL: Biletix NFD (U+0307) ve büyük "İ" (U+0130) varyantlarını
 * tüm şehir adları (İzmir, İstanbul, …) için ortak eşleşmeye dönüştürür.
 */
public final class EventRepositoryCityMatch {

    /** e.city ile :city — iki taraf aynı çevrimle kıyaslanır. */
    public static final String JPQL_EQ_PARAM =
            "LOWER(FUNCTION('replace', FUNCTION('replace', e.city, '\u0307', ''), '\u0130', 'i')) = "
                    + "LOWER(FUNCTION('replace', FUNCTION('replace', :city, '\u0307', ''), '\u0130', 'i'))";

    /** Sabit "istanbul" ile; parametre yok. */
    public static final String JPQL_EQ_ISTANBUL =
            "LOWER(FUNCTION('replace', FUNCTION('replace', e.city, '\u0307', ''), '\u0130', 'i')) = 'istanbul'";

    private EventRepositoryCityMatch() {
    }
}
