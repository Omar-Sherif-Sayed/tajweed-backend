package io.github.omarsherifsayed.tajweed.business.reciter.schema.response;

import io.github.omarsherifsayed.tajweed.business.reciter.enums.AudioProvider;
import io.github.omarsherifsayed.tajweed.business.reciter.enums.RecitationStyle;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A recitation as a list shows it.
 *
 * @param id the recitation
 * @param reciterId its reciter
 * @param nameAr the source's label in Arabic («حفص عن عاصم - مرتل»)
 * @param nameEn the source's label in English
 * @param style murattal, mujawwad or muallim; absent when the source does not state it
 * @param riwayah its riwayah
 * @param provider who serves it: mp3quran (one file per surah) or EveryAyah (one file per ayah)
 * @param listeningOnly {@code true} when its riwayah is not the one of the displayed text: it is played without following or highlighting the Hafs text ({@code AGENTS.md} §6.7)
 * @param surahCount how many surahs it holds
 * @param complete whether it holds all 114
 * @param hasTimings whether per-ayah timings exist (needed to follow the text in a surah file)
 */
@Schema(description = "A recitation: label, style, riwayah, completeness, timings.")
public record DtoRecitationSummary(int id, int reciterId, String nameAr, String nameEn, RecitationStyle style,
                                   DtoRiwayah riwayah, AudioProvider provider, boolean listeningOnly, int surahCount,
                                   boolean complete, boolean hasTimings) {
}
