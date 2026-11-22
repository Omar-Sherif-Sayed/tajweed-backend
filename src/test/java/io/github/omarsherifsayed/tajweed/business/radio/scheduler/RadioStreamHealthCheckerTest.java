package io.github.omarsherifsayed.tajweed.business.radio.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.sun.net.httpserver.HttpServer;
import io.github.omarsherifsayed.tajweed.business.radio.repository.RadioStreamCheckRepository;
import io.github.omarsherifsayed.tajweed.business.radio.repository.RadioStreamRepository;
import io.github.omarsherifsayed.tajweed.business.reciter.scheduler.AudioHealthProperties;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Proves the check of one radio stream against a small local HTTP server (no network): {@code 200} with an audio type
 * and some bytes works; HTML, an empty body, {@code 500} or no answer does not.
 */
class RadioStreamHealthCheckerTest {

    /** A local server. */
    private static HttpServer server;

    /** Its base address. */
    private static String base;

    /** The checker under test. */
    private static RadioStreamHealthChecker checker;

    /**
     * Starts the local server: {@code /live} (audio bytes), {@code /page} (HTML), {@code /empty} (audio, no bytes),
     * {@code /busy} (500).
     *
     * @throws IOException if it cannot start
     */
    @BeforeAll
    static void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/live", e -> {
            e.getResponseHeaders().add("Content-Type", "audio/mpeg");
            e.sendResponseHeaders(200, 0);
            try (OutputStream out = e.getResponseBody()) {
                out.write(new byte[8192]);
            }
            e.close();
        });
        server.createContext("/page", e -> {
            byte[] html = "<html></html>".getBytes();
            e.getResponseHeaders().add("Content-Type", "text/html");
            e.sendResponseHeaders(200, html.length);
            try (OutputStream out = e.getResponseBody()) {
                out.write(html);
            }
            e.close();
        });
        server.createContext("/empty", e -> {
            e.getResponseHeaders().add("Content-Type", "audio/mpeg");
            e.sendResponseHeaders(200, -1);
            e.close();
        });
        server.createContext("/busy", e -> {
            e.sendResponseHeaders(500, -1);
            e.close();
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
        checker = new RadioStreamHealthChecker(mock(RadioStreamRepository.class), mock(RadioStreamCheckRepository.class),
                new AudioHealthProperties(true, "0 30 3 * * *", Duration.ofSeconds(5)),
                Clock.fixed(Instant.parse("2026-11-22T03:30:00Z"), ZoneOffset.UTC));
    }

    /**
     * Stops the local server.
     */
    @AfterAll
    static void stop() {
        server.stop(0);
    }

    @Test
    @DisplayName("Audio bytes work; HTML, no bytes, 500 or a closed port do not")
    void checksOneStream() {
        assertThat(checker.sendsAudio(base + "/live")).isTrue();
        assertThat(checker.sendsAudio(base + "/page")).isFalse();
        assertThat(checker.sendsAudio(base + "/empty")).isFalse();
        assertThat(checker.sendsAudio(base + "/busy")).isFalse();
        assertThat(checker.sendsAudio("http://127.0.0.1:1/none")).isFalse();
    }
}
