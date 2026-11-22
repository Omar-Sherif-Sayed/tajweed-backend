package io.github.omarsherifsayed.tajweed.business.hadith.schema.response;

import io.github.omarsherifsayed.tajweed.business.source.schema.response.DtoCredit;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Every category, with the credit.
 *
 * @param categories the categories, by id
 * @param credit     HadeethEnc, to be shown
 */
@Schema(description = "The categories of hadiths and their credit.")
public record DtoHadithCategories(List<DtoHadithCategory> categories, DtoCredit credit) {
}
