package io.github.omarsherifsayed.tajweed.business.hadith.schema.response;

import io.github.omarsherifsayed.tajweed.business.source.schema.response.DtoCredit;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * One hadith: always in Arabic, and in the request's language when HadeethEnc publishes that translation.
 *
 * @param id          HadeethEnc's id
 * @param arabic      the Arabic text
 * @param translation the translation in the request's language; absent for Arabic or when HadeethEnc has none
 *                    (then the screen shows the Arabic with a note — {@code AGENTS.md} §6.10)
 * @param categoryIds the categories it belongs to
 * @param sourceUrl   its page on HadeethEnc, shown as a link
 * @param credit      HadeethEnc, to be shown
 */
@Schema(description = "A hadith: Arabic always, the translation in the request's language when it exists, its link and credit.")
public record DtoHadith(int id, DtoHadithText arabic, DtoHadithText translation, List<Integer> categoryIds,
                        String sourceUrl, DtoCredit credit) {
}
