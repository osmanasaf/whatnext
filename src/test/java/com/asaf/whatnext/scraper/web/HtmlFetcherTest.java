package com.asaf.whatnext.scraper.web;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static java.time.Duration.ofSeconds;

class HtmlFetcherTest {

    /** Başlığı gönderip gövdede susan sunucu thread'i sonsuza dek kilitlememeli (2026-09-20 olayı). */
    @Test
    void stalledBodyTimesOut() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            ex.sendResponseHeaders(200, 0);
            ex.getResponseBody().write("<html>".getBytes());
            ex.getResponseBody().flush();
            try {
                Thread.sleep(60_000);
            } catch (InterruptedException ignored) {
            }
            ex.close();
        });
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
            assertTimeoutPreemptively(ofSeconds(10),
                    () -> assertThrows(IOException.class, () -> HtmlFetcher.get(url, 1000)));
        } finally {
            server.stop(0);
        }
    }
}
