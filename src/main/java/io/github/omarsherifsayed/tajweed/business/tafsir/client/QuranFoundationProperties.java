package io.github.omarsherifsayed.tajweed.business.tafsir.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The settings of the Quran Foundation's API, under {@code tajweed.quran-foundation}. The id and the secret come only
 * from the environment — never from a file of the repository.
 *
 * @param enabled      whether the scheduled sync runs ({@code QF_SYNC_ENABLED})
 * @param clientId     the app's client id ({@code QF_CLIENT_ID}); empty means the tafsir sync is off
 * @param clientSecret the app's secret ({@code QF_CLIENT_SECRET}); empty means the tafsir sync is off
 * @param env          {@code prelive} (surahs 1 – 2 only) or {@code production} ({@code QF_ENV})
 * @param cron         when the daily look runs; it syncs when the last sync is older than {@code refreshAfter}
 * @param refreshAfter how old the last sync may be — 6 days, inside their limit of 7
 * @param timeout      how long one request may take
 * @param authUrl      the token service; empty means the one of {@code env}
 * @param apiUrl       the Content API, up to {@code /content}; empty means the one of {@code env}
 * @param onlyTafsirs  for the live test only: these resource ids instead of {@code content/reviewed/tafsirs.csv} —
 *                     pre-live does not offer the chosen tafsirs, so the mechanism is checked on one it offers
 */
@Validated
@ConfigurationProperties(prefix = "tajweed.quran-foundation")
public record QuranFoundationProperties(boolean enabled, String clientId, String clientSecret, @NotBlank String env,
                                        @NotBlank String cron, @NotNull Duration refreshAfter,
                                        @NotNull Duration timeout, String authUrl, String apiUrl,
                                        java.util.List<Integer> onlyTafsirs) {

    /**
     * Whether the credentials are set.
     *
     * @return {@code true} when both the id and the secret are present
     */
    public boolean hasCredentials() {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }

    /**
     * The token service of the environment.
     *
     * @return its address
     */
    public String tokenUrl() {
        if (authUrl != null && !authUrl.isBlank()) {
            return authUrl;
        }
        return production() ? "https://oauth2.quran.foundation/oauth2/token"
                : "https://prelive-oauth2.quran.foundation/oauth2/token";
    }

    /**
     * The Content API of the environment, up to {@code /content} (relative addresses in their answers start after it).
     *
     * @return its address
     */
    public String contentUrl() {
        if (apiUrl != null && !apiUrl.isBlank()) {
            return apiUrl;
        }
        return production() ? "https://apis.quran.foundation/content" : "https://apis-prelive.quran.foundation/content";
    }

    /**
     * Whether the production environment is chosen.
     *
     * @return {@code true} for {@code production}
     */
    public boolean production() {
        return "production".equalsIgnoreCase(env);
    }
}
