package io.github.omarsherifsayed.tajweed.business.quran.schema.response;

import io.github.omarsherifsayed.tajweed.business.source.schema.response.DtoContentSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * The ayahs that start on a page of the Madinah mushaf. The lines of the page (where each line breaks) are not part
 * of this answer: the apps draw the page with the mushaf layout they ship ({@code P0-12}).
 *
 * @param page       the page, 1 to 604
 * @param ayahs      the ayahs that start on it, in order
 * @param textSource where the text comes from
 */
@Schema(description = "The ayahs that start on a page of the Madinah mushaf, and the source of the text.")
public record DtoPage(int page, List<DtoAyah> ayahs, DtoContentSource textSource) {
}
