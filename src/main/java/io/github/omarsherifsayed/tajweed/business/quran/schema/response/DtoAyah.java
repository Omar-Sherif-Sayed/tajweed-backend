package io.github.omarsherifsayed.tajweed.business.quran.schema.response;

import io.github.omarsherifsayed.tajweed.business.quran.enums.SajdahType;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * An ayah: its text exactly as Tanzil gives it, and its place in the printed Madinah mushaf.
 *
 * <p>The text is never changed by the server ({@code AGENTS.md} §6.1). When {@code basmalaWordCount} is 4, the text
 * begins with the basmala: a client that shows the basmala as a heading leaves out these first four words; a client
 * that shows the text as one block shows it whole.
 *
 * @param id               the ayah's place in the mushaf, 1 to 6,236
 * @param surahNumber      its surah
 * @param number           its number in the surah
 * @param text             the Tanzil Uthmani text, verbatim
 * @param basmalaWordCount 4 when the text begins with the basmala, otherwise 0
 * @param page             its page, 1 to 604
 * @param juz              its juz, 1 to 30
 * @param hizb             its hizb, 1 to 60
 * @param hizbQuarter      its hizb quarter, 1 to 240
 * @param sajdah           the type of its place of prostration, absent when it has none
 */
@Schema(description = "An ayah: verbatim Tanzil text and its page, juz, hizb, quarter and sajdah.")
public record DtoAyah(int id, int surahNumber, int number, String text, int basmalaWordCount, int page, int juz,
                      int hizb, int hizbQuarter, SajdahType sajdah) {
}
