package com.asaf.whatnext.scraper.web;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Sunucuda render edilen scraper sayfalarını düz HTTP ile çeker; tarayıcı gerekmez.
 * Biletino, Bubilet ve Biletinial bunu kullanır — üçünün de liste ve detay sayfaları sunucu
 * tarafında üretiliyor (2026-09-09 canlı doğrulama).
 *
 * <p>İki tuzak canlı doğrulamada tespit edildi:
 * <ul>
 *   <li>Biletino kısa User-Agent'ı 403 ile reddediyor — {@code "Mozilla/5.0"} yetmiyor,
 *       tam tarayıcı dizgesi gerekiyor.</li>
 *   <li>{@code Jsoup.connect(...)} aynı adrese tam User-Agent ile bile 403 alıyor;
 *       {@code java.net.http} ile aynı istek 200 dönüyor. Bu yüzden indirme HttpClient'la,
 *       ayrıştırma Jsoup'la yapılır.</li>
 * </ul>
 */
public final class HtmlFetcher {

    public static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private HtmlFetcher() {
    }

    public static Document get(String url, int timeoutMs) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofMillis(timeoutMs))
                .GET()
                .build();

        HttpResponse<String> response = send(CLIENT, request);
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " — " + url);
        }
        return Jsoup.parse(response.body(), url);
    }

    /**
     * {@code HttpClient.send} gibi, ama isteğin {@code timeout}'u gövde dahil toplam süreye uygulanır.
     * JDK'nın timeout'u yalnızca yanıt başlıklarına kadar geçerli; başlığı gönderip gövdede susan
     * sunucu {@code send}'i sonsuza dek bekletir. 2026-09-20'de Biletinial taramasında böyle takıldı,
     * tek scheduler thread'i kilitlendi ve bir hafta boyunca hiçbir paylaşım görevi çalışmadı.
     */
    public static HttpResponse<String> send(HttpClient client, HttpRequest request)
            throws IOException, InterruptedException {
        long deadlineMs = request.timeout().orElse(Duration.ofSeconds(30)).toMillis();
        CompletableFuture<HttpResponse<String>> future =
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        try {
            return future.get(deadlineMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new IOException("Toplam süre aşıldı (" + deadlineMs + " ms) — " + request.uri(), e);
        } catch (InterruptedException e) {
            future.cancel(true);
            throw e;
        } catch (ExecutionException e) {
            throw e.getCause() instanceof IOException io ? io : new IOException(e.getCause());
        }
    }

    /** Hata durumunda {@code null} — çağıran mevcut değerini korumak istediğinde. */
    public static Document getOrNull(String url, int timeoutMs) {
        try {
            return get(url, timeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
