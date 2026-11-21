package io.github.omarsherifsayed.tajweed.business.reciter.schema.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * When one ayah starts and ends in the surah file.
 *
 * @param ayah the ayah, from 0 (0 = what is recited before ayah 1)
 * @param startMs its start, in milliseconds
 * @param endMs its end, in milliseconds
 */
@Schema(description = "An ayah's start and end in milliseconds.")
public record DtoAyahTiming(int ayah, int startMs, int endMs) {
}
