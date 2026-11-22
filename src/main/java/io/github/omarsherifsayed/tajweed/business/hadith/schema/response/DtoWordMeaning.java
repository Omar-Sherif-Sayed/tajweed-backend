package io.github.omarsherifsayed.tajweed.business.hadith.schema.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The meaning of a word of the hadith, as HadeethEnc gives it.
 *
 * @param word    the word
 * @param meaning its meaning
 */
@Schema(description = "A word of the hadith and its meaning.")
public record DtoWordMeaning(String word, String meaning) {
}
