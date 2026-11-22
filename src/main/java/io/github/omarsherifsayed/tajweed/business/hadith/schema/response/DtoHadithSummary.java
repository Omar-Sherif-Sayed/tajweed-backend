package io.github.omarsherifsayed.tajweed.business.hadith.schema.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A hadith as a list shows it.
 *
 * @param id            HadeethEnc's id
 * @param title         its title in the request's language, or in Arabic
 * @param titleLanguage the language of {@code title}
 */
@Schema(description = "A hadith in a list: its id and title.")
public record DtoHadithSummary(int id, String title, String titleLanguage) {
}
