package io.github.omarsherifsayed.tajweed.business.radio.schema.response;

import io.github.omarsherifsayed.tajweed.business.reciter.enums.SourceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * A stream of a station, in the order the players try it (F11).
 *
 * @param priority      1 is tried first
 * @param url           the stream's address, played directly by the client
 * @param status        the last health check: absent before the first check
 * @param lastCheckedAt when it was last checked, absent before the first check
 */
@Schema(description = "A radio stream: its address and health.")
public record DtoRadioStream(int priority, String url, SourceStatus status, Instant lastCheckedAt) {
}
