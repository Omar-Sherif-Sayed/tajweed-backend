package io.github.omarsherifsayed.tajweed.business.reciter.scheduler;

import io.github.omarsherifsayed.tajweed.business.reciter.entity.AudioSource;
import io.github.omarsherifsayed.tajweed.business.reciter.entity.AudioSourceCheck;
import io.github.omarsherifsayed.tajweed.business.reciter.repository.AudioSourceCheckRepository;
import io.github.omarsherifsayed.tajweed.business.reciter.repository.AudioSourceRepository;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checks every audio source once: a {@code HEAD} request for one sample file (the first ayah of Al-Fatihah, or its
 * surah file), and records the result in {@code audio_source_check}.
 *
 * <p>Only the address and the answer's status are used; nothing is downloaded. A source counts as working when the
 * file answers {@code 200} with an audio content type. The clients try the sources in order anyway (F11), so a wrong
 * result never stops playback — it only moves a failing source down the list a client may choose to try.
 */
@Slf4j
@Component
public class AudioSourceHealthChecker {

    /** The sources to check. */
    private final AudioSourceRepository audioSourceRepository;

    /** Where the results go. */
    private final AudioSourceCheckRepository audioSourceCheckRepository;

    /** The HTTP client (no cookies, follows redirects, time-limited). */
    private final HttpClient httpClient;

    /** How long one request may take. */
    private final AudioHealthProperties properties;

    /** The time of the checks (a fixed clock in tests). */
    private final Clock clock;

    /**
     * Creates the checker.
     *
     * @param audioSourceRepository      reads the sources
     * @param audioSourceCheckRepository writes their health
     * @param properties                 the timeout
     * @param clock                      the time of the checks
     */
    public AudioSourceHealthChecker(AudioSourceRepository audioSourceRepository,
                                    AudioSourceCheckRepository audioSourceCheckRepository,
                                    AudioHealthProperties properties, Clock clock) {
        this.audioSourceRepository = audioSourceRepository;
        this.audioSourceCheckRepository = audioSourceCheckRepository;
        this.properties = properties;
        this.clock = clock;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(properties.timeout())
                .build();
    }

    /**
     * Checks every source and records the results.
     *
     * @return how many sources answered with audio
     */
    @Transactional
    public int checkAll() {
        List<AudioSource> sources = audioSourceRepository.findAllByOrderByIdAsc();
        int ok = 0;
        for (AudioSource source : sources) {
            boolean works = answersWithAudio(sampleUrl(source.getUrlTemplate()));
            Instant now = clock.instant();
            AudioSourceCheck check = audioSourceCheckRepository.findById(source.getId())
                    .orElseGet(() -> new AudioSourceCheck(source.getId()));
            check.record(works, now);
            audioSourceCheckRepository.save(check);
            if (works) {
                ok++;
            } else {
                log.warn("audio source {} failed its check ({} in a row)", source.getId(), check.getConsecutiveFailures());
            }
        }
        log.info("audio sources checked: {} of {} answered with audio", ok, sources.size());
        return ok;
    }

    /**
     * The sample file of a source: surah 1, ayah 1.
     *
     * @param urlTemplate the source's address template
     * @return the address of the sample file
     */
    static String sampleUrl(String urlTemplate) {
        return urlTemplate.replace("{surah}", "001").replace("{ayah}", "001");
    }

    /**
     * Whether an address answers {@code HEAD} with {@code 200} and an audio content type.
     *
     * @param url the address
     * @return {@code true} when it does; {@code false} on any other answer, a time-out or a network error
     */
    boolean answersWithAudio(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .timeout(properties.timeout())
                .build();
        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200
                    && response.headers().firstValue("Content-Type").orElse("").startsWith("audio/");
        } catch (IOException e) {
            log.debug("audio check of {} failed: {}", url, e.toString());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
