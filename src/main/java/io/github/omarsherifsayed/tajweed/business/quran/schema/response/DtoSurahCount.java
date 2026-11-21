package io.github.omarsherifsayed.tajweed.business.quran.schema.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * How many of the found ayahs are in one surah (for the results grouped by surah).
 *
 * @param surahNumber the surah, 1 to 114
 * @param count       its ayahs among the results
 */
@Schema(description = "How many found ayahs are in one surah.")
public record DtoSurahCount(int surahNumber, int count) {
}
