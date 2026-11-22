package io.github.omarsherifsayed.tajweed.business.hadith.controller;

import io.github.omarsherifsayed.tajweed.business.hadith.schema.response.DtoHadith;
import io.github.omarsherifsayed.tajweed.business.hadith.schema.response.DtoHadithCategories;
import io.github.omarsherifsayed.tajweed.business.hadith.schema.response.DtoHadithPage;
import io.github.omarsherifsayed.tajweed.business.hadith.service.HadithRetrievalService;
import io.github.omarsherifsayed.tajweed.general.config.HttpCaching;
import io.github.omarsherifsayed.tajweed.general.i18n.MessageService;
import io.github.omarsherifsayed.tajweed.general.schema.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /v1/hadith-categories} and {@code /v1/hadiths} — HadeethEnc's encyclopedia (F13, F29, F35): the categories,
 * the hadiths of a category, and one hadith in Arabic with its translation in the request's language.
 *
 * <p>An unknown category answers {@code 404 NOT_FOUND_HADITH_CATEGORY}, an unknown hadith {@code 404 NOT_FOUND_HADITH}.
 * The content is refreshed from HadeethEnc at most daily, so it is cached for a day.
 */
@Tag(name = "Hadith", description = "HadeethEnc's encyclopedia: categories, hadiths, grades, explanations, translations.")
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class HadithRetrievalController {

    /** Builds the answers. */
    private final HadithRetrievalService hadithRetrievalService;

    /** Reads the success message in the request's language. */
    private final MessageService messages;

    /**
     * Every category.
     *
     * @param locale the request's language
     * @return {@code 200} with the categories and the credit
     */
    @Operation(summary = "The categories of hadiths, titled in the request's language (or Arabic)")
    @GetMapping(path = "/hadith-categories", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse<DtoHadithCategories>> getCategories(Locale locale) {
        return ok(locale, hadithRetrievalService.getCategories(locale));
    }

    /**
     * The hadiths of a category.
     *
     * @param categoryId the category
     * @param page       the page, from 0
     * @param size       the page's size, 1 to 100
     * @param locale     the request's language
     * @return {@code 200} with one page
     */
    @Operation(summary = "One page of the hadiths of a category and its sub-categories")
    @GetMapping(path = "/hadith-categories/{categoryId}/hadiths", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse<DtoHadithPage>> getHadiths(@PathVariable int categoryId,
                                                                  @RequestParam(defaultValue = "0") @Min(0) int page,
                                                                  @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                                                  Locale locale) {
        return ok(locale, hadithRetrievalService.getHadiths(categoryId, page, size, locale));
    }

    /**
     * One hadith.
     *
     * @param hadithId the hadith (HadeethEnc's id)
     * @param locale   the request's language
     * @return {@code 200} with the hadith
     */
    @Operation(summary = "One hadith: Arabic, and the translation in the request's language when HadeethEnc has it")
    @GetMapping(path = "/hadiths/{hadithId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse<DtoHadith>> getHadith(@PathVariable int hadithId, Locale locale) {
        return ok(locale, hadithRetrievalService.getHadith(hadithId, locale));
    }

    /**
     * A successful answer, cached for a day.
     *
     * @param locale the request's language
     * @param data   the answer's data
     * @param <T>    its type
     * @return {@code 200}, varying with the language
     */
    private <T> ResponseEntity<BaseResponse<T>> ok(Locale locale, T data) {
        return ResponseEntity.ok()
                .cacheControl(HttpCaching.CONTENT)
                .varyBy(HttpHeaders.ACCEPT_LANGUAGE)
                .body(BaseResponse.ok(messages.get(locale, "response.success"), data));
    }
}
