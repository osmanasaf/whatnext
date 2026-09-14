package com.asaf.whatnext.services.scoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PopularityCatalogTest {

    private static PopularityCatalog catalog(String json) throws Exception {
        return new PopularityCatalog(new ObjectMapper().readTree(json));
    }

    @Test
    void aliasesSplitParenthesesAndAmpersand_dropGenericWords() {
        assertThat(PopularityCatalog.aliases("Kel Diva (Haluk Bilginer & Zuhal Olcay)"))
                .containsExactly("kel diva", "haluk bilginer", "zuhal olcay");
        assertThat(PopularityCatalog.aliases("Kuğu Gölü (Bale)")).containsExactly("kugu golu");
    }

    @Test
    void multiWordPerformerMatchesAnywhere_singleWordOnlyAtStart() throws Exception {
        PopularityCatalog c = catalog("{\"topArtists\":[\"Cem Yılmaz\",\"Güneş\",\"Zengin Mutfağı (Şener Şen)\"]}");

        assertThat(c.performerScore("Cem Yılmaz - Diamond Elite Platinum Plus")).isEqualTo(10);
        assertThat(c.performerScore("Konuşanlar - CEM YILMAZ Özel")).isEqualTo(10);
        assertThat(c.performerScore("Güneş Konseri")).isEqualTo(10);
        assertThat(c.performerScore("Doğan Güneş Tiyatrosu")).isZero();
        assertThat(c.performerScore("Güneşin Sofrası")).isZero();
        // Farklı kaynakta Türkçe harfsiz yazım
        assertThat(c.performerScore("Sener Sen ile Bir Aksam")).isEqualTo(10);
        assertThat(c.performerScore(null, "Zengin Mutfağı")).isEqualTo(10);
    }

    @Test
    void venueMatchIgnoresSpacesAndTurkishLetters() throws Exception {
        PopularityCatalog c = catalog("{\"topVenues\":[\"KadıköySahne\",\"Zorlu PSM\"]}");

        assertThat(c.venueScore("Kadıköy Sahne")).isEqualTo(10);
        assertThat(c.venueScore("Zorlu PSM - Turkcell Sahnesi")).isEqualTo(10);
        assertThat(c.venueScore("Bir Kafe")).isZero();
        assertThat(c.venueScore(null)).isZero();
    }

    @Test
    void realConfigLoads() {
        PopularityCatalog c = new PopularityCatalog();
        assertThat(c.performerScore("Baturay Özdemir - Stand Up")).isEqualTo(10);
        assertThat(c.venueScore("Harbiye Cemil Topuzlu Açık Hava Tiyatrosu")).isEqualTo(10);
    }
}
