package io.github.omarsherifsayed.tajweed.business.quran.schema.response;

import io.github.omarsherifsayed.tajweed.business.source.schema.response.DtoContentSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * A page of the Madinah mushaf: its ayahs and its lines as printed (1405H) — everything a client needs to draw the
 * page as the printed copy. Every page of this mushaf begins with a new ayah (the import checks it), so the ayahs
 * that start on the page are all the ayahs on it.
 *
 * @param page          the page, 1 to 604
 * @param ayahs         the ayahs on it — all of them start on it — in order
 * @param lines         its lines, top to bottom (15; 8 on pages 1 and 2)
 * @param textSource    where the text comes from
 * @param layoutSource  where the line breaks come from
 */
@Schema(description = "A page of the Madinah mushaf: its ayahs, its lines as printed (1405H), and their sources.")
public record DtoPage(int page, List<DtoAyah> ayahs, List<DtoMushafLine> lines,
        DtoContentSource textSource, DtoContentSource layoutSource) {
}
