package io.github.omarsherifsayed.tajweed.general.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.omarsherifsayed.tajweed.general.config.AppProperties;
import io.github.omarsherifsayed.tajweed.general.exception.ErrorCode;
import io.github.omarsherifsayed.tajweed.general.i18n.MessageService;
import io.github.omarsherifsayed.tajweed.general.schema.BaseResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.LocaleResolver;
import tools.jackson.databind.json.JsonMapper;

/**
 * Limits how many requests one client may send to the public API ({@code /v1/**}) each minute.
 *
 * <p>Each IP address gets a token bucket (Bucket4j) of {@code tajweed.rate-limit.requests-per-minute} tokens, refilled
 * evenly over the minute. When the bucket is empty the answer is {@code 429} with {@link ErrorCode#RATE_LIMITED} and a
 * {@code Retry-After} header. The buckets live in memory only (Caffeine), are forgotten ten minutes after a client's
 * last request, and the IP addresses are never written anywhere ({@code AGENTS.md} §5: no trackers).
 *
 * <p>Behind a reverse proxy, set {@code server.forward-headers-strategy=native} so that the client's address, not the
 * proxy's, is used ({@code README.md}).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    /** Only the public API is limited; health checks and the OpenAPI pages are not. */
    private static final String LIMITED_PREFIX = "/v1/";

    /** The seconds a limited client is told to wait: the time one token takes to come back, at most a minute. */
    private static final long RETRY_AFTER_SECONDS = 60;

    /** One bucket per client address, kept in memory and forgotten after ten minutes without requests. */
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(100_000)
            .build();

    /** How many requests one client may send each minute. */
    private final int requestsPerMinute;

    /** Decides the language of the error message (filters run before Spring MVC sets the language). */
    private final LocaleResolver localeResolver;

    /** Reads the error message in that language. */
    private final MessageService messages;

    /** Writes the JSON error body. */
    private final JsonMapper jsonMapper;

    /**
     * Creates the filter.
     *
     * @param properties     the application's settings (the limit per minute)
     * @param localeResolver decides the language of the error message
     * @param messages       reads the error message
     * @param jsonMapper     writes the JSON error body
     */
    public RateLimitFilter(AppProperties properties, LocaleResolver localeResolver, MessageService messages,
                           JsonMapper jsonMapper) {
        this.requestsPerMinute = properties.rateLimit().requestsPerMinute();
        this.localeResolver = localeResolver;
        this.messages = messages;
        this.jsonMapper = jsonMapper;
    }

    /**
     * Limits only the public API.
     *
     * @param request the request
     * @return {@code true} when the path is outside {@code /v1/}
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(LIMITED_PREFIX);
    }

    /**
     * Takes one token from the client's bucket, or answers {@code 429} when it is empty.
     *
     * @param request  the request
     * @param response the response
     * @param chain    the rest of the filters and the controller
     * @throws ServletException if a later filter fails
     * @throws IOException      if the answer cannot be written
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Bucket bucket = buckets.get(request.getRemoteAddr(), key -> newBucket());
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
            return;
        }
        ErrorCode code = ErrorCode.RATE_LIMITED;
        String text = messages.get(localeResolver.resolveLocale(request), code.getMessageKey());
        response.setStatus(code.getStatus().value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(RETRY_AFTER_SECONDS));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(response.getOutputStream(), BaseResponse.error(code.name(), text));
    }

    /**
     * A full bucket for a new client.
     *
     * @return {@code requestsPerMinute} tokens, refilled evenly over a minute
     */
    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(requestsPerMinute)
                .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
