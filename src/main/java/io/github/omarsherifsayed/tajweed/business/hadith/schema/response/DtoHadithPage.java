package io.github.omarsherifsayed.tajweed.business.hadith.schema.response;

import io.github.omarsherifsayed.tajweed.business.source.schema.response.DtoCredit;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * One page of the hadiths of a category.
 *
 * @param categoryId the category
 * @param hadiths    the hadiths of the page, by id
 * @param page       the page, from 0
 * @param size       the page's size
 * @param total      the hadiths of the category and its sub-categories
 * @param credit     HadeethEnc, to be shown
 */
@Schema(description = "One page of the hadiths of a category.")
public record DtoHadithPage(int categoryId, List<DtoHadithSummary> hadiths, int page, int size, int total, DtoCredit credit) {
}
