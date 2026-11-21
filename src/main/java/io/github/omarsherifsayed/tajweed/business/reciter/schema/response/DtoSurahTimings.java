package io.github.omarsherifsayed.tajweed.business.reciter.schema.response;

import io.github.omarsherifsayed.tajweed.business.source.schema.response.DtoContentSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * The ayah timings of one surah of a recitation (mp3quran's, checked against the Tanzil ayah count).
 *
 * @param recitationId the recitation
 * @param surahNumber the surah
 * @param timings one entry per ayah, in order
 * @param source where the timings come from
 */
@Schema(description = "The ayah timings of one surah of a recitation.")
public record DtoSurahTimings(int recitationId, int surahNumber, List<DtoAyahTiming> timings, DtoContentSource source) {
}
