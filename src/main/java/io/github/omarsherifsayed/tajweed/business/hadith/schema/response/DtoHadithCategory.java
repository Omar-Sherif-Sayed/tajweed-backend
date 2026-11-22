package io.github.omarsherifsayed.tajweed.business.hadith.schema.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A category of hadiths.
 *
 * @param id            HadeethEnc's id
 * @param parentId      the parent category, absent for a root
 * @param title         its title in the request's language, or in Arabic when HadeethEnc has none in that language
 * @param titleLanguage the language of {@code title}
 * @param hadithCount   the hadiths in it and its sub-categories
 */
@Schema(description = "A category of hadiths: title (in the request's language, or Arabic), parent, count.")
public record DtoHadithCategory(int id, Integer parentId, String title, String titleLanguage, int hadithCount) {
}
