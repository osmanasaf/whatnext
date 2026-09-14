package com.asaf.whatnext.scraper.biletix;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BiletixPopularityIndexTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode json(String s) throws Exception {
        return MAPPER.readTree(s);
    }

    /** Canlı yanıtın biçimi (2026-09-14): elements[] içinde event XOR group. */
    private static JsonNode hotList(int groupsBefore, String lastElement) throws Exception {
        StringBuilder sb = new StringBuilder("{\"status\":\"SUCCESS\",\"data\":{\"elements\":[");
        for (int i = 0; i < groupsBefore; i++) {
            sb.append("{\"event\":null,\"group\":{\"name\":\"Dolgu Grup ").append(i).append("\"}},");
        }
        sb.append(lastElement).append("]}}");
        return json(sb.toString());
    }

    @Test
    void rankTiers() {
        assertThat(BiletixPopularityIndex.pointsForRank(1)).isEqualTo(20);
        assertThat(BiletixPopularityIndex.pointsForRank(10)).isEqualTo(20);
        assertThat(BiletixPopularityIndex.pointsForRank(11)).isEqualTo(15);
        assertThat(BiletixPopularityIndex.pointsForRank(26)).isEqualTo(10);
        assertThat(BiletixPopularityIndex.pointsForRank(51)).isEqualTo(6);
    }

    @Test
    void matchesSingleEventByEventCode() throws Exception {
        BiletixPopularityIndex index = BiletixPopularityIndex.from(List.of(hotList(0,
                "{\"event\":{\"eventCode\":\"5ST01\",\"eventName\":\"Maximum Sunar: Yann Tiersen\"},\"group\":null}")), List.of());

        assertThat(index.pointsFor(json("{\"id\":\"5ST01\",\"sname\":\"Başka Başlık\"}"))).isEqualTo(20);
        assertThat(index.pointsFor(json("{\"id\":\"5XXXX\",\"sname\":\"Alakasız\"}"))).isZero();
    }

    /** Solr grup adları sonda \r\n taşıyabiliyor ("Sami Yusuf\r\n") — normalize edilip eşleşmeli. */
    @Test
    void matchesGroupByGroupParentOrArtistField() throws Exception {
        BiletixPopularityIndex index = BiletixPopularityIndex.from(List.of(hotList(11,
                "{\"event\":null,\"group\":{\"name\":\"Sami Yusuf\"}}")), List.of());

        assertThat(index.pointsFor(json("{\"id\":\"5LN07\",\"sname\":\"Konser\",\"group\":[\"Sami Yusuf\\r\\n\"]}"))).isEqualTo(15);
        assertThat(index.pointsFor(json("{\"id\":\"5LN08\",\"sname\":\"Konser\",\"artist\":[\"\",\"SAMİ YUSUF\"]}"))).isEqualTo(15);
    }

    @Test
    void multiWordPrefixMatchesTourSuffix_butSingleWordDoesNot() throws Exception {
        BiletixPopularityIndex index = BiletixPopularityIndex.from(List.of(json("""
                {"data":{"elements":[
                  {"event":null,"group":{"name":"Zengin Mutfağı Turne"}},
                  {"event":null,"group":{"name":"Model"}}
                ]}}""")), List.of());

        assertThat(index.pointsFor(json("{\"id\":\"1\",\"sname\":\"Zengin Mutfağı\"}"))).isEqualTo(20);
        assertThat(index.pointsFor(json("{\"id\":\"2\",\"sname\":\"Model Uçak Atölyesi\"}"))).isZero();
        assertThat(index.pointsFor(json("{\"id\":\"3\",\"sname\":\"Model\"}"))).isEqualTo(20);
    }

    @Test
    void featuredGetsFixedPoints_andHotRankWinsWhenBoth() throws Exception {
        JsonNode featured = json("""
                {"data":{"elements":[
                  {"event":null,"group":{"name":"Kaan Sekban Stand Up"}},
                  {"event":null,"group":{"name":"Teoman"}}
                ]}}""");
        JsonNode hot = json("{\"data\":{\"elements\":[{\"event\":null,\"group\":{\"name\":\"Teoman\"}}]}}");
        BiletixPopularityIndex index = BiletixPopularityIndex.from(List.of(hot), List.of(featured));

        assertThat(index.pointsFor(json("{\"id\":\"a\",\"sname\":\"Kaan Sekban Stand Up\"}")))
                .isEqualTo(BiletixPopularityIndex.FEATURED_POINTS);
        assertThat(index.pointsFor(json("{\"id\":\"b\",\"sname\":\"Teoman\"}"))).isEqualTo(20);
    }

    /** Canlı probe bulgusu: mekân serisi grubu altındaki her küçük konser 20 almamalı; tek sanatçının çok gecesi almalı. */
    @Test
    void venueSeriesIsDamped_butMultiNightActIsNot() throws Exception {
        BiletixPopularityIndex index = BiletixPopularityIndex.from(List.of(json("""
                {"data":{"elements":[
                  {"event":null,"group":{"name":"Blind İstanbul Konserleri"}},
                  {"event":null,"group":{"name":"Mabel Matiz Konserleri"}},
                  {"event":{"eventCode":"5ST01","eventName":"Yann Tiersen","eventGroupName":"Blind İstanbul Konserleri"},"group":null}
                ]}}""")), List.of());

        List<JsonNode> docs = new java.util.ArrayList<>();
        for (String t : List.of("Maymun Collective", "Demir Demirkan", "Palmiyeler", "Seyyah", "Malt")) {
            docs.add(json("{\"id\":\"" + t.hashCode() + "\",\"sname\":\"" + t + "\",\"group\":[\"Blind İstanbul Konserleri\"]}"));
        }
        for (String id : List.of("m1", "m2", "m3", "m4")) {
            docs.add(json("{\"id\":\"" + id + "\",\"sname\":\"Mabel Matiz\",\"group\":[\"Mabel Matiz Konserleri\"]}"));
        }
        docs.add(json("{\"id\":\"5ST01\",\"sname\":\"Yann Tiersen\",\"group\":[\"Blind İstanbul Konserleri\"]}"));

        assertThat(index.pointsFor(docs.get(0))).as("sönümlemeden önce seri grubu tam puan").isEqualTo(20);

        BiletixPopularityIndex damped = index.dampSeries(docs);

        assertThat(damped.pointsFor(docs.get(0))).isEqualTo(BiletixPopularityIndex.SERIES_POINTS);
        assertThat(damped.pointsFor(docs.get(5))).isEqualTo(20);
        // Etkinlik koduyla doğrudan çok satan: seri sönümlemesinden etkilenmez
        assertThat(damped.pointsFor(docs.get(9))).isEqualTo(20);
    }

    @Test
    void emptyIndex() throws Exception {
        assertThat(BiletixPopularityIndex.EMPTY.isEmpty()).isTrue();
        assertThat(BiletixPopularityIndex.EMPTY.pointsFor(json("{\"id\":\"5ST01\"}"))).isZero();
    }
}
