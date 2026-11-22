package io.github.omarsherifsayed.tajweed.business.hadith.scheduler;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The settings of the HadeethEnc sync, under {@code tajweed.hadeethenc}.
 *
 * @param enabled      whether the scheduled sync runs ({@code HADEETHENC_SYNC_ENABLED})
 * @param baseUrl      the API's address ({@code https://hadeethenc.com/api/v1})
 * @param cron         when the daily look runs; it syncs only when the last full sync is older than
 *                     {@code refreshAfter}, or did not finish
 * @param delay        the pause before every request — one a second, to be gentle with their server
 * @param refreshAfter how old a stored text may be before it is fetched again (30 days)
 * @param timeout      how long one request may take
 */
@Validated
@ConfigurationProperties(prefix = "tajweed.hadeethenc")
public record HadeethEncProperties(boolean enabled, @NotBlank String baseUrl, @NotBlank String cron,
                                   @NotNull Duration delay, @NotNull Duration refreshAfter, @NotNull Duration timeout) {
}
