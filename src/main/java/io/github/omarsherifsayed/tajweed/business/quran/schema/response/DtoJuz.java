package io.github.omarsherifsayed.tajweed.business.quran.schema.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Where a juz starts, as the index of the mushaf shows it.
 *
 * @param number           the juz, 1 to 30
 * @param startSurahNumber the surah of its first ayah
 * @param startAyahNumber  its first ayah's number in that surah
 * @param startPage        the page where it starts
 */
@Schema(description = "Where a juz starts: its first ayah and page.")
public record DtoJuz(int number, int startSurahNumber, int startAyahNumber, int startPage) {
}
