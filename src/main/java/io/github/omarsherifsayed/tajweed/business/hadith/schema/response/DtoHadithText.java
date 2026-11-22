package io.github.omarsherifsayed.tajweed.business.hadith.schema.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * A hadith in one language, exactly as HadeethEnc publishes it.
 *
 * @param language      the language code
 * @param title         its title
 * @param hadeeth       the hadith's text
 * @param hadeethIntro  the narrator's introduction, absent when none
 * @param attribution   who reported it, absent when none
 * @param grade         its grade as HadeethEnc states it — never shown apart from the hadith
 * @param explanation   the explanation, absent when none
 * @param hints         the benefits
 * @param wordsMeanings the word meanings
 * @param reference     the references (book, volume, page, number), absent when none
 */
@Schema(description = "A hadith in one language: text, attribution, grade, explanation, benefits, word meanings, references.")
public record DtoHadithText(String language, String title, String hadeeth, String hadeethIntro, String attribution,
                            String grade, String explanation, List<String> hints, List<DtoWordMeaning> wordsMeanings,
                            String reference) {
}
