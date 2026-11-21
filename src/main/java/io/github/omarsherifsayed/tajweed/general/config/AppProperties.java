package io.github.omarsherifsayed.tajweed.general.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The application's own settings, read from {@code application.properties} under the prefix {@code tajweed}.
 *
 * <p>Every value can be overridden by an environment variable ({@code README.md} lists them). The values are checked
 * when the application starts: a missing or invalid value stops it with a clear message.
 *
 * @param app       facts about the application shown by {@code GET /v1/app-info}
 * @param cors      which web origins may call the API from a browser
 * @param rateLimit how many requests one client may send
 */
@Validated
@ConfigurationProperties(prefix = "tajweed")
public record AppProperties(@NotNull @Valid App app, @NotNull @Valid Cors cors, @NotNull @Valid RateLimit rateLimit) {

    /**
     * Facts about the application.
     *
     * @param apiVersion the version of the public API, also the {@code /v1} in every path
     * @param authorLink the author's one public link (his LinkedIn page, {@code AGENTS.md} §2)
     */
    public record App(@NotBlank String apiVersion, @NotBlank String authorLink) {
    }

    /**
     * Cross-origin settings for the web application.
     *
     * @param allowedOrigins the origins allowed to call {@code /v1/**} from a browser, for example
     *                       {@code https://tajweed.example}; never {@code *}
     */
    public record Cors(@NotEmpty List<@NotBlank String> allowedOrigins) {
    }

    /**
     * Limits that protect the public API.
     *
     * @param requestsPerMinute how many requests one client (one IP address) may send to {@code /v1/**} each minute
     */
    public record RateLimit(@Min(1) int requestsPerMinute) {
    }
}
