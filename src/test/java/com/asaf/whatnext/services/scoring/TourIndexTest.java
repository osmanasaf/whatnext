package com.asaf.whatnext.services.scoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TourIndexTest {

    @Test
    void key_dropsCityVaryingSubtitle() {
        assertThat(TourIndex.key("Hikayeden Adamlar - Mahalle")).isEqualTo("hikayeden adamlar");
        assertThat(TourIndex.key("Redd (Akustik)")).isEqualTo("redd");
        assertThat(TourIndex.key("Kaan Sekban: Stand Up")).isEqualTo("kaan sekban");
        // Tire boşluksuzsa isim parçasıdır ("Stand-up"), bölünmez.
        assertThat(TourIndex.key("Kadıköy Stand-up Gecesi")).isEqualTo("kadıköy stand up gecesi");
    }

    @Test
    void build_countsDistinctCities_scoreRewardsTouring() throws Exception {
        var docs = new ObjectMapper().readTree("""
                [ {"sname":"Redd","city":["İstanbul"]}, {"sname":"Redd","city":["Ankara"]},
                  {"sname":"Redd (Akustik)","city":["İzmir"]}, {"sname":"Redd","city":["Ankara"]},
                  {"sname":"Küçük Caz Gecesi","city":["İstanbul"]} ]""");
        Map<String, Integer> index = TourIndex.build(docs);
        assertThat(index).containsEntry("redd", 3).containsEntry("küçük caz gecesi", 1);

        TourIndex tours = new TourIndex();
        tours.load(index);
        assertThat(tours.score("Redd")).isEqualTo(4);            // 3 şehir → (3-1)*2
        assertThat(tours.score("Küçük Caz Gecesi")).isZero();    // tek şehir puan almaz
        assertThat(tours.score("Bilinmeyen")).isZero();
        tours.load(Map.of("hikayeden adamlar", 19));
        assertThat(tours.score("Hikayeden Adamlar - Mahalle")).isEqualTo(20); // tavan
    }
}
