package io.github.omarsherifsayed.tajweed.business.tafsir.client;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Calls the Quran Foundation's API as their quick-start describes: a token from the OAuth2 service (client
 * credentials, HTTP Basic, scopes {@value #SCOPES}), kept until 30 seconds before it ends; every Content API call with
 * {@code x-auth-token} and {@code x-client-id}; on {@code 401} the token is renewed once and the call repeated once.
 * The secret is never logged.
 */
@Slf4j
@Component
public class QuranFoundationClient {

    /** The scopes the tafsir sync needs. */
    static final String SCOPES = "content.tafsirs.read content.sync.read";

    /** Renew the token this long before it ends. */
    private static final long MARGIN_SECONDS = 30;

    /** The settings. */
    private final QuranFoundationProperties properties;

    /** The HTTP client. */
    private final HttpClient http;

    /** The time. */
    private final Clock clock;

    /** Reads the answers. */
    private final JsonMapper json = JsonMapper.builder().build();

    /** The current token, or {@code null}. */
    private String token;

    /** When the current token must be renewed. */
    private Instant renewAt = Instant.EPOCH;

    /**
     * Creates the client.
     *
     * @param properties the settings
     * @param clock      the time
     */
    public QuranFoundationClient(QuranFoundationProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(properties.timeout()).build();
    }

    /**
     * Calls the Content API.
     *
     * @param pathAndQuery the address after {@code /content} (for example {@code /api/v4/resources/tafsirs}), or an
     *                     absolute address the API gave
     * @return the answer's JSON
     * @throws QuranFoundationException when the answer is not {@code 200}, with the API's error type
     */
    public synchronized JsonNode get(String pathAndQuery) {
        String url = pathAndQuery.startsWith("http") ? pathAndQuery : properties.contentUrl() + pathAndQuery;
        HttpResponse<String> response = send(url);
        if (response.statusCode() == 401) {
            token = null;
            response = send(url);
        }
        if (response.statusCode() != 200) {
            String type = "";
            String detail = "";
            try {
                JsonNode error = json.readTree(response.body());
                type = error.path("type").asString(error.path("error").asString(""));
                detail = error.path("message").asString(error.path("error_description").asString(""));
            } catch (RuntimeException ignored) {
                // not JSON — the status says enough
            }
            throw new QuranFoundationException(response.statusCode(), type,
                    url.replaceAll("\\?.*", "") + (detail.isBlank() ? "" : " — " + detail));
        }
        return json.readTree(response.body());
    }

    /**
     * Sends one Content API request with a valid token.
     *
     * @param url the address
     * @return the answer
     */
    private HttpResponse<String> send(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().timeout(properties.timeout())
                .header("x-auth-token", token())
                .header("x-client-id", properties.clientId())
                .header("Accept", "application/json")
                .build();
        return call(request);
    }

    /**
     * The token, renewed when it is about to end.
     *
     * @return a valid access token
     */
    private String token() {
        if (token != null && clock.instant().isBefore(renewAt)) {
            return token;
        }
        String basic = Base64.getEncoder().encodeToString((properties.clientId() + ":" + properties.clientSecret())
                .getBytes(StandardCharsets.UTF_8));
        String form = "grant_type=client_credentials&scope=" + URLEncoder.encode(SCOPES, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.tokenUrl()))
                .timeout(properties.timeout())
                .header("Authorization", "Basic " + basic)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        HttpResponse<String> response = call(request);
        if (response.statusCode() != 200) {
            throw new QuranFoundationException(response.statusCode(), "token", "the token service");
        }
        JsonNode answer = json.readTree(response.body());
        token = answer.get("access_token").asString();
        renewAt = clock.instant().plusSeconds(Math.max(0, answer.path("expires_in").asLong(3600) - MARGIN_SECONDS));
        log.info("Quran Foundation token obtained ({})", properties.production() ? "production" : "prelive");
        return token;
    }

    /**
     * Sends a request.
     *
     * @param request the request
     * @return the answer
     */
    private HttpResponse<String> call(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new QuranFoundationException(0, e.getClass().getSimpleName(), request.uri().getHost());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new QuranFoundationException(0, "interrupted", request.uri().getHost());
        }
    }
}
