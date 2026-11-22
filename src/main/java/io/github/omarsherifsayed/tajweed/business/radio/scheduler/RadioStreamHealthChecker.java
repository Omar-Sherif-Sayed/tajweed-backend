package io.github.omarsherifsayed.tajweed.business.radio.scheduler;

import io.github.omarsherifsayed.tajweed.business.radio.entity.RadioStream;
import io.github.omarsherifsayed.tajweed.business.radio.entity.RadioStreamCheck;
import io.github.omarsherifsayed.tajweed.business.radio.repository.RadioStreamCheckRepository;
import io.github.omarsherifsayed.tajweed.business.radio.repository.RadioStreamRepository;
import io.github.omarsherifsayed.tajweed.business.reciter.scheduler.AudioHealthProperties;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checks every radio stream once: a {@code GET} whose first bytes are read and then closed — a live stream has no
 * end — and records the result in {@code radio_stream_check}.
 *
 * <p>A stream works when it answers {@code 200} with an audio content type and sends bytes. The server of mp3quran's
 * channels answers {@code 500} at times, so a stream is tried up to {@value #TRIES} times before the check counts as
 * failed.
 */
@Slf4j
@Component
public class RadioStreamHealthChecker {

    /** Tries per stream in one check. */
    static final int TRIES = 3;

    /** How many bytes are read before the stream is closed. */
    private static final int SAMPLE_BYTES = 4096;

    /** The streams to check. */
    private final RadioStreamRepository radioStreamRepository;

    /** Where the results go. */
    private final RadioStreamCheckRepository radioStreamCheckRepository;

    /** The HTTP client (no cookies, follows redirects, time-limited). */
    private final HttpClient httpClient;

    /** How long one request may take. */
    private final AudioHealthProperties properties;

    /** The time of the checks (a fixed clock in tests). */
    private final Clock clock;

    /**
     * Creates the checker.
     *
     * @param radioStreamRepository      reads the streams
     * @param radioStreamCheckRepository writes their health
     * @param properties                 the timeout (shared with the audio sources' check)
     * @param clock                      the time of the checks
     */
    public RadioStreamHealthChecker(RadioStreamRepository radioStreamRepository,
                                    RadioStreamCheckRepository radioStreamCheckRepository,
                                    AudioHealthProperties properties, Clock clock) {
        this.radioStreamRepository = radioStreamRepository;
        this.radioStreamCheckRepository = radioStreamCheckRepository;
        this.properties = properties;
        this.clock = clock;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(properties.timeout())
                .build();
    }

    /**
     * Checks every stream and records the results.
     *
     * @return how many streams answered with audio
     */
    @Transactional
    public int checkAll() {
        List<RadioStream> streams = radioStreamRepository.findAll();
        int ok = 0;
        for (RadioStream stream : streams) {
            boolean works = false;
            for (int i = 0; i < TRIES && !works; i++) {
                works = sendsAudio(stream.getUrl());
            }
            RadioStreamCheck check = radioStreamCheckRepository.findById(stream.getId())
                    .orElseGet(() -> new RadioStreamCheck(stream.getId()));
            check.record(works, clock.instant());
            radioStreamCheckRepository.save(check);
            if (works) {
                ok++;
            } else {
                log.warn("radio stream {} failed its check ({} in a row)", stream.getId(), check.getConsecutiveFailures());
            }
        }
        log.info("radio streams checked: {} of {} answered with audio", ok, streams.size());
        return ok;
    }

    /**
     * Whether an address answers {@code GET} with {@code 200}, an audio content type and some bytes.
     *
     * @param url the stream's address
     * @return {@code true} when it does; {@code false} on any other answer, a time-out or a network error
     */
    boolean sendsAudio(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().timeout(properties.timeout()).build();
        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                return response.statusCode() == 200
                        && response.headers().firstValue("Content-Type").orElse("").startsWith("audio/")
                        && body.readNBytes(SAMPLE_BYTES).length > 0;
            }
        } catch (IOException e) {
            log.debug("radio check of {} failed: {}", url, e.toString());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
