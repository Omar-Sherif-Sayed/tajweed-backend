package io.github.omarsherifsayed.tajweed.business.quran.schema.response;

import io.github.omarsherifsayed.tajweed.business.source.schema.response.DtoContentSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * A surah with all its ayahs, and the source of the text — which the clients must show with a link (Tanzil's licence,
 * {@code AGENTS.md} §7.1).
 *
 * @param surah      the surah
 * @param ayahs      its ayahs, in order
 * @param textSource where the text comes from
 */
@Schema(description = "A surah, its ayahs in order, and the source of the text.")
public record DtoSurahAyahs(DtoSurah surah, List<DtoAyah> ayahs, DtoContentSource textSource) {
}
