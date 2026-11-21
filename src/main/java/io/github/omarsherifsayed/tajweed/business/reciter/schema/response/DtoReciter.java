package io.github.omarsherifsayed.tajweed.business.reciter.schema.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * A reciter with his recitations.
 *
 * @param id the reciter
 * @param nameAr his name in Arabic (mp3quran's)
 * @param nameEn his name in English (mp3quran's)
 * @param recitations his recitations, Hafs first
 */
@Schema(description = "A reciter and his recitations.")
public record DtoReciter(int id, String nameAr, String nameEn, List<DtoRecitationSummary> recitations) {
}
