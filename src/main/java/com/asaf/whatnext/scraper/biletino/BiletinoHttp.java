package com.asaf.whatnext.scraper.biletino;

import com.asaf.whatnext.config.BiletinoScraperConfig;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Biletino sayfalarını düz HTTP ile çeker (arama sonucu da etkinlik detayı da sunucuda render
 * ediliyor, tarayıcı gerekmiyor).
 *
 * <p>İki tuzak canlı doğrulamada (2026-09-09) tespit edildi:
 * <ul>
 *   <li>Site kısa/şüpheli User-Agent'ı 403 ile reddediyor — {@code "Mozilla/5.0"} yetmiyor,
 *       tam tarayıcı dizgesi gerekiyor.</li>
 *   <li>{@code Jsoup.connect(...)} tam User-Agent ile bile 403 alıyor; {@code java.net.http}
 *       ile aynı istek 200 dönüyor. Bu yüzden indirme HttpClient'la, ayrıştırma Jsoup'la yapılır.</li>
 * </ul>
 */
public final class BiletinoHttp {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private BiletinoHttp() {
    }

    public static Document get(String url, int timeoutMs) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", BiletinoScraperConfig.USER_AGENT)
                .timeout(Duration.ofMillis(timeoutMs))
                .GET()
                .build();

        HttpResponse<String> response = CLIENT.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("Biletino HTTP " + response.statusCode() + " — " + url);
        }
        return Jsoup.parse(response.body(), url);
    }
}
