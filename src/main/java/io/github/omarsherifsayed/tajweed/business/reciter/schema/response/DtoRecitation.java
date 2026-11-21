package io.github.omarsherifsayed.tajweed.business.reciter.schema.response;

import io.github.omarsherifsayed.tajweed.business.source.schema.response.DtoContentSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * A recitation with everything a player needs: the surahs it holds, its sources in order, and the credit to show.
 *
 * @param recitation the recitation
 * @param reciterNameAr its reciter's name in Arabic
 * @param reciterNameEn its reciter's name in English
 * @param surahs the numbers of the surahs it holds, in order
 * @param sources its audio sources, in the order to try them
 * @param credits the sources of this catalogue, to be shown with it
 */
@Schema(description = "A recitation, its surahs, its audio sources in order and its credits.")
public record DtoRecitation(DtoRecitationSummary recitation, String reciterNameAr, String reciterNameEn, List<Integer> surahs,
                            List<DtoAudioSource> sources, List<DtoContentSource> credits) {
}
