package io.github.omarsherifsayed.tajweed.business.reciter.scheduler;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The settings of the daily health check of the audio sources, under {@code tajweed.audio-health}.
 *
 * @param enabled whether the check runs at all ({@code AUDIO_HEALTH_ENABLED}; off in the tests)
 * @param cron    when it runs ({@code AUDIO_HEALTH_CRON}, a Spring cron expression; default 03:30 UTC every day)
 * @param timeout how long one request may take before it counts as a failure
 */
@Validated
@ConfigurationProperties(prefix = "tajweed.audio-health")
public record AudioHealthProperties(boolean enabled, @NotBlank String cron, @NotNull Duration timeout) {
}
