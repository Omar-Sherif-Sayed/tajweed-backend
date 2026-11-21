package io.github.omarsherifsayed.tajweed.business.reciter.schema.response;

import io.github.omarsherifsayed.tajweed.business.reciter.enums.AudioProvider;
import io.github.omarsherifsayed.tajweed.business.reciter.enums.AudioSourceKind;
import io.github.omarsherifsayed.tajweed.business.reciter.enums.SourceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * An audio source, in the order the players try it (F11).
 *
 * @param priority 1 is tried first
 * @param kind surah files or ayah files
 * @param provider who serves it
 * @param urlTemplate a file's address, with {@code {surah}} and {@code {ayah}} to be written as three digits
 * @param bitrateKbps the bitrate the source states, absent when it states none
 * @param downloadAllowed whether a client may keep the files for offline listening (F28)
 * @param status the last health check: absent before the first check
 * @param lastCheckedAt when it was last checked, absent before the first check
 */
@Schema(description = "An audio source: its address template, provider, bitrate and health.")
public record DtoAudioSource(int priority, AudioSourceKind kind, AudioProvider provider, String urlTemplate, Integer bitrateKbps,
                             boolean downloadAllowed, SourceStatus status, Instant lastCheckedAt) {
}
