package io.github.omarsherifsayed.tajweed.business.quran.controller;

import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoAyah;
import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoSurah;
import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoSurahAyahs;
import io.github.omarsherifsayed.tajweed.business.quran.service.SurahRetrievalService;
import io.github.omarsherifsayed.tajweed.general.config.HttpCaching;
import io.github.omarsherifsayed.tajweed.general.i18n.MessageService;
import io.github.omarsherifsayed.tajweed.general.schema.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /v1/surahs} — the surahs, a surah with its ayahs, and one ayah.
 *
 * <p>A surah number outside 1 – 114 answers {@code 400 INVALID_REQUEST}; an ayah number the surah does not have
 * answers {@code 404 NOT_FOUND_AYAH}. The content changes only with a migration, so the answers may be cached for a
 * day ({@link HttpCaching#CONTENT}).
 */
@Tag(name = "Quran", description = "Surahs, ayahs, pages and juz of the Madinah mushaf — text verbatim from Tanzil.")
@RestController
@RequestMapping("/v1/surahs")
@RequiredArgsConstructor
public class SurahRetrievalController {

    /** Builds the answers. */
    private final SurahRetrievalService surahRetrievalService;

    /** Reads the success message in the request's language. */
    private final MessageService messages;

    /**
     * The 114 surahs, for the index of the mushaf.
     *
     * @param locale the request's language (for the message)
     * @return {@code 200} with every surah in order
     */
    @Operation(summary = "The 114 surahs: names, Makki / Madani, order of revelation, ayahs, first page")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse<List<DtoSurah>>> getSurahs(Locale locale) {
        return ok(locale, surahRetrievalService.getSurahs());
    }

    /**
     * A surah with all its ayahs.
     *
     * @param surahNumber the surah, 1 to 114
     * @param locale      the request's language (for the message)
     * @return {@code 200} with the surah, its ayahs and the source of the text
     */
    @Operation(summary = "A surah with all its ayahs (verbatim Tanzil text) and the source of the text")
    @GetMapping(path = "/{surahNumber}/ayahs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse<DtoSurahAyahs>> getSurahAyahs(
            @PathVariable @Min(1) @Max(114) int surahNumber, Locale locale) {
        return ok(locale, surahRetrievalService.getSurahAyahs(surahNumber));
    }

    /**
     * One ayah.
     *
     * @param surahNumber the surah, 1 to 114
     * @param ayahNumber  the ayah's number in it, from 1
     * @param locale      the request's language (for the message)
     * @return {@code 200} with the ayah
     */
    @Operation(summary = "One ayah (verbatim Tanzil text) with its page, juz, hizb, quarter and sajdah")
    @GetMapping(path = "/{surahNumber}/ayahs/{ayahNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse<DtoAyah>> getAyah(@PathVariable @Min(1) @Max(114) int surahNumber,
                                                         @PathVariable @Min(1) int ayahNumber, Locale locale) {
        return ok(locale, surahRetrievalService.getAyah(surahNumber, ayahNumber));
    }

    /**
     * A successful, cacheable answer.
     *
     * @param locale the request's language
     * @param data   the answer's data
     * @param <T>    its type
     * @return {@code 200}, cacheable for a day, varying with the language
     */
    private <T> ResponseEntity<BaseResponse<T>> ok(Locale locale, T data) {
        return ResponseEntity.ok()
                .cacheControl(HttpCaching.CONTENT)
                .varyBy(HttpHeaders.ACCEPT_LANGUAGE)
                .body(BaseResponse.ok(messages.get(locale, "response.success"), data));
    }
}
