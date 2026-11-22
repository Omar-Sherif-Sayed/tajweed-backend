package io.github.omarsherifsayed.tajweed.business.tafsir.schema.response;

import io.github.omarsherifsayed.tajweed.business.source.schema.response.DtoCredit;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * The tafsirs of one ayah.
 *
 * @param surah   the surah
 * @param ayah    the ayah
 * @param tafsirs the texts, in the list's order — a tafsir without text for this ayah is left out
 * @param credit  the Quran Foundation, to be shown
 */
@Schema(description = "The tafsirs of one ayah, with the credit.")
public record DtoAyahTafsirs(int surah, int ayah, List<DtoAyahTafsir> tafsirs, DtoCredit credit) {
}
