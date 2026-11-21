package io.github.omarsherifsayed.tajweed.general.config;

import java.time.Duration;
import org.springframework.http.CacheControl;

/**
 * How long clients and caches may keep each kind of answer — one place, so every controller says the same.
 *
 * <p>Every {@code /v1/**} answer also carries an {@code ETag} ({@link WebConfig}): after the time below, a client asks
 * again with {@code If-None-Match} and receives {@code 304 Not Modified} when nothing changed.
 */
public final class HttpCaching {

    /** Content (the Quran text, its metadata, the sources): it changes only with a migration — a day. */
    public static final CacheControl CONTENT = CacheControl.maxAge(Duration.ofDays(1)).cachePublic();

    /** Facts about the application: they change only with a release — an hour. */
    public static final CacheControl APP_INFO = CacheControl.maxAge(Duration.ofHours(1)).cachePublic();

    /** Content with run-time state (a recitation with its sources' daily health check) — an hour. */
    public static final CacheControl CHECKED_DAILY = CacheControl.maxAge(Duration.ofHours(1)).cachePublic();

    /**
     * Not instantiated: this class only holds constants.
     */
    private HttpCaching() {
    }
}
