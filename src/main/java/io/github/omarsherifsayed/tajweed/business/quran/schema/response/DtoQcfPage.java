package io.github.omarsherifsayed.tajweed.business.quran.schema.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * A page drawn in the King Fahd Complex's QCF V1 fonts (mushaf 2 of the Quran Foundation): the glyphs of its text
 * lines, each in the page's own font ({@code p{page}}), and the basmala's glyphs (ayah 1:1, drawn in the page-1 font —
 * the Quran Foundation's advice). The text copied and searched stays the Tanzil text.
 *
 * @param lines     the text lines, top to bottom (the lines not given are the surah headers and basmalas)
 * @param basmala   the glyphs of the basmala, for the page-1 font
 * @param credit    the credit their terms ask for
 * @param creditUrl where the credit links
 */
@Schema(description = "A page's glyphs in the King Fahd QCF V1 fonts, from the Quran Foundation.")
public record DtoQcfPage(List<DtoQcfLine> lines, String basmala, String credit, String creditUrl) {

    /**
     * A text line.
     *
     * @param line   the line, 1 to 15
     * @param glyphs its glyphs, right to left
     */
    @Schema(description = "A text line of the page in QCF V1 glyphs.")
    public record DtoQcfLine(int line, List<DtoQcfGlyph> glyphs) {
    }

    /**
     * A word or an ayah's number.
     *
     * @param glyph  its characters in the page's font
     * @param ayahId the ayah it belongs to
     * @param end    {@code true} for the ayah's number
     */
    @Schema(description = "A word, or an ayah's number, as a glyph of the page's font.")
    public record DtoQcfGlyph(String glyph, int ayahId, boolean end) {
    }
}
