package io.github.omarsherifsayed.tajweed.business.quran.schema.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * An ayah that contains the searched words.
 *
 * @param ayah         the ayah, its text verbatim
 * @param matchedWords the words of {@code ayah.text} that hold the match — indexes of its space-separated parts,
 *                     from 0; empty when they cannot be told exactly (then the whole ayah is highlighted)
 */
@Schema(description = "An ayah that contains the searched words, and which of its words to highlight.")
public record DtoSearchHit(DtoAyah ayah, List<Integer> matchedWords) {
}
