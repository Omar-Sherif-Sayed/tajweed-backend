package io.github.omarsherifsayed.tajweed.business.quran.schema.response;

import io.github.omarsherifsayed.tajweed.business.quran.enums.RevelationType;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A surah, as the index of the mushaf shows it ({@code AGENTS.md} §8.1: order, name, Makki / Madani, ayahs, first page).
 *
 * <p>The names come from Tanzil's metadata. The clients show {@code nameAr} for the Arabic-script languages and
 * {@code nameTransliteration} for the others; a named source for surah names in every language is still to be chosen
 * ({@code LANG-02}).
 *
 * @param number              the surah's number, 1 to 114
 * @param nameAr              its Arabic name
 * @param nameTransliteration its name in Latin letters
 * @param nameEn              the English meaning of its name
 * @param revelationType      Makki or Madani
 * @param revelationOrder     its place in the order of revelation
 * @param ayahCount           how many ayahs it has
 * @param startPage           the page of the Madinah mushaf where it starts
 */
@Schema(description = "A surah: number, names, Makki / Madani, order of revelation, ayahs, first page.")
public record DtoSurah(int number, String nameAr, String nameTransliteration, String nameEn,
                       RevelationType revelationType, int revelationOrder, int ayahCount, int startPage) {
}
