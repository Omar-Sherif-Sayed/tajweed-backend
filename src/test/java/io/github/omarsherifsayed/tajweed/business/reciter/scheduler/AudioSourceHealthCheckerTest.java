package io.github.omarsherifsayed.tajweed.business.reciter.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.sun.net.httpserver.HttpServer;
import io.github.omarsherifsayed.tajweed.business.reciter.entity.AudioSourceCheck;
import io.github.omarsherifsayed.tajweed.business.reciter.enums.SourceStatus;
import io.github.omarsherifsayed.tajweed.business.reciter.repository.AudioSourceCheckRepository;
import io.github.omarsherifsayed.tajweed.business.reciter.repository.AudioSourceRepository;
import java.io.IOException;
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
 * Proves the health check of one address against a small local HTTP server (no network): {@code 200} with audio
 * works; another status, another content type or no answer does not. Also proves the sample address and how three
 * failures in a row mark a source failing.
 */
class AudioSourceHealthCheckerTest {

    /** A local server with three paths. */
    private static HttpServer server;

    /** Its base address. */
    private static String base;

    /** The checker under test. */
    private static AudioSourceHealthChecker checker;

    /**
     * Starts the local server: {@code /audio.mp3} (200, audio), {@code /page} (200, HTML), {@code /missing} (404).
     *
     * @throws IOException if it cannot start
     */
    @BeforeAll
    static void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/audio.mp3", e -> {
            e.getResponseHeaders().add("Content-Type", "audio/mpeg");
            e.sendResponseHeaders(200, -1);
            e.close();
        });
        server.createContext("/page", e -> {
            e.getResponseHeaders().add("Content-Type", "text/html");
            e.sendResponseHeaders(200, -1);
            e.close();
        });
        server.createContext("/missing", e -> {
            e.sendResponseHeaders(404, -1);
            e.close();
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
        checker = new AudioSourceHealthChecker(mock(AudioSourceRepository.class), mock(AudioSourceCheckRepository.class),
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
    @DisplayName("200 with an audio content type works; 200 with HTML, 404 or a closed port does not")
    void checksOneAddress() {
        assertThat(checker.answersWithAudio(base + "/audio.mp3")).isTrue();
        assertThat(checker.answersWithAudio(base + "/page")).isFalse();
        assertThat(checker.answersWithAudio(base + "/missing")).isFalse();
        assertThat(checker.answersWithAudio("http://127.0.0.1:1/none.mp3")).isFalse();
    }

    @Test
    @DisplayName("The sample file is surah 1, ayah 1, written with three digits")
    void buildsTheSampleAddress() {
        assertThat(AudioSourceHealthChecker.sampleUrl("https://cdn.mp3quran.net/audio/x/r1/{surah}.mp3"))
                .isEqualTo("https://cdn.mp3quran.net/audio/x/r1/001.mp3");
        assertThat(AudioSourceHealthChecker.sampleUrl("https://everyayah.com/data/F/{surah}{ayah}.mp3"))
                .isEqualTo("https://everyayah.com/data/F/001001.mp3");
    }

    @Test
    @DisplayName("Three failures in a row mark a source FAILING; one answer with audio makes it OK again")
    void marksAFailingSource() {
        AudioSourceCheck check = new AudioSourceCheck(7);
        Instant t = Instant.parse("2026-11-22T03:30:00Z");
        check.record(false, t);
        check.record(false, t);
        assertThat(check.getStatus()).isEqualTo(SourceStatus.OK);
        check.record(false, t);
        assertThat(check.getStatus()).isEqualTo(SourceStatus.FAILING);
        assertThat(check.getConsecutiveFailures()).isEqualTo((short) 3);
        check.record(true, t);
        assertThat(check.getStatus()).isEqualTo(SourceStatus.OK);
        assertThat(check.getLastOkAt()).isEqualTo(t);
        assertThat(check.getConsecutiveFailures()).isZero();
    }
}
