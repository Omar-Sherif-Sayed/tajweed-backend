package io.github.omarsherifsayed.tajweed.business.reciter.controller;

import io.github.omarsherifsayed.tajweed.business.reciter.schema.response.DtoRecitation;
import io.github.omarsherifsayed.tajweed.business.reciter.schema.response.DtoSurahTimings;
import io.github.omarsherifsayed.tajweed.business.reciter.service.ReciterRetrievalService;
import io.github.omarsherifsayed.tajweed.general.config.HttpCaching;
import io.github.omarsherifsayed.tajweed.general.i18n.MessageService;
import io.github.omarsherifsayed.tajweed.general.schema.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /v1/recitations} — one recitation with its surahs and audio sources, and its ayah timings per surah.
 *
 * <p>An unknown id answers {@code 404 NOT_FOUND_RECITATION}; a surah without timings {@code 404 NOT_FOUND_TIMINGS}; a
 * surah number outside 1 – 114 {@code 400 INVALID_REQUEST}. A recitation carries the sources' health, which changes
 * daily, so it is cached for an hour; the timings change only with a migration (a day).
 */
@Tag(name = "Reciters", description = "Reciters, recitations (riwayah, style, surahs), audio sources and ayah timings.")
@RestController
@RequestMapping("/v1/recitations")
@RequiredArgsConstructor
public class RecitationRetrievalController {

    /** Builds the answers. */
    private final ReciterRetrievalService reciterRetrievalService;

    /** Reads the success message in the request's language. */
    private final MessageService messages;

    /**
     * One recitation.
     *
     * @param recitationId the recitation
     * @param locale       the request's language (for the message)
     * @return {@code 200} with its surahs, its sources in the order to try them, and its credits
     */
    @Operation(summary = "A recitation: riwayah, surahs, audio sources in order with their health, credits")
    @GetMapping(path = "/{recitationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse<DtoRecitation>> getRecitation(@PathVariable int recitationId, Locale locale) {
        return ok(locale, HttpCaching.CHECKED_DAILY, reciterRetrievalService.getRecitation(recitationId));
    }

    /**
     * The ayah timings of one surah.
     *
     * @param recitationId the recitation
     * @param surahNumber  the surah, 1 to 114
     * @param locale       the request's language (for the message)
     * @return {@code 200} with one entry per ayah, from 0
     */
    @Operation(summary = "The ayah timings of one surah of a recitation (milliseconds in the surah file)")
    @GetMapping(path = "/{recitationId}/surahs/{surahNumber}/timings", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse<DtoSurahTimings>> getTimings(@PathVariable int recitationId,
                                                                    @PathVariable @Min(1) @Max(114) int surahNumber,
                                                                    Locale locale) {
        return ok(locale, HttpCaching.CONTENT, reciterRetrievalService.getTimings(recitationId, surahNumber));
    }

    /**
     * A successful, cacheable answer.
     *
     * @param locale  the request's language
     * @param caching how long it may be cached
     * @param data    the answer's data
     * @param <T>     its type
     * @return {@code 200}, varying with the language
     */
    private <T> ResponseEntity<BaseResponse<T>> ok(Locale locale, CacheControl caching, T data) {
        return ResponseEntity.ok()
                .cacheControl(caching)
                .varyBy(HttpHeaders.ACCEPT_LANGUAGE)
                .body(BaseResponse.ok(messages.get(locale, "response.success"), data));
    }
}
