package io.github.omarsherifsayed.tajweed.business.quran.schema.response;

import io.github.omarsherifsayed.tajweed.business.quran.enums.MushafLineKind;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A line of the printed Madinah mushaf, 1405H. A text line is a run of the words of the ayahs' {@code text}, split at
 * single spaces and counted from 0: from {@code firstWord} of {@code firstAyahId} to {@code lastWord} of
 * {@code lastAyahId}, both included — the client draws those words exactly as received, and the ayah's number after
 * an ayah's last word. The basmala's words in front of an ayah 1 are never on a text line: the {@code BASMALA} line
 * draws them.
 *
 * @param line        the line, 1 to 15
 * @param kind        what it holds
 * @param surahNumber the surah of a header or a basmala, otherwise absent
 * @param firstAyahId a text line's first ayah (the ayah's {@code id}), otherwise absent
 * @param firstWord   its first word, from 0
 * @param lastAyahId  a text line's last ayah
 * @param lastWord    its last word, from 0
 */
@Schema(description = "A line of the printed mushaf: a surah header, the basmala, or a run of the ayahs' words.")
public record DtoMushafLine(int line, MushafLineKind kind, Integer surahNumber, Integer firstAyahId,
        Integer firstWord, Integer lastAyahId, Integer lastWord) {
}
