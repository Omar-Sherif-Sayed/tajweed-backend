package io.github.omarsherifsayed.tajweed.business.tafsir.schema.response;

import io.github.omarsherifsayed.tajweed.business.source.schema.response.DtoCredit;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * The tafsirs, with the credit.
 *
 * @param tafsirs the tafsirs, in the list's order (empty until the first sync)
 * @param credit  the Quran Foundation, to be shown
 */
@Schema(description = "The tafsirs and their credit.")
public record DtoTafsirs(List<DtoTafsir> tafsirs, DtoCredit credit) {
}
