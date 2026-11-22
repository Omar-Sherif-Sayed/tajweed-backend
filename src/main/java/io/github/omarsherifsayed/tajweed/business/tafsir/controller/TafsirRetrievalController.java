package io.github.omarsherifsayed.tajweed.business.tafsir.controller;

import io.github.omarsherifsayed.tajweed.business.tafsir.schema.response.DtoAyahTafsirs;
import io.github.omarsherifsayed.tajweed.business.tafsir.schema.response.DtoTafsirs;
import io.github.omarsherifsayed.tajweed.business.tafsir.service.TafsirRetrievalService;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /v1/tafsirs} and {@code /v1/surahs/{n}/ayahs/{a}/tafsirs} — the tafsirs (F5) and every tafsir of an ayah
 * (F26), synced from the Quran Foundation.
 *
 * <p>An ayah outside its surah answers {@code 404 NOT_FOUND_AYAH}; an unknown tafsir {@code 404 NOT_FOUND_TAFSIR}. The
 * texts change at most with a weekly sync, so they are cached for a day.
 */
@Tag(name = "Tafsir", description = "Tafsirs synced from the Quran Foundation: the list, and every tafsir of an ayah.")
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class TafsirRetrievalController {

    /** Builds the answers. */
    private final TafsirRetrievalService tafsirRetrievalService;

    /** Reads the success message in the request's language. */
    private final MessageService messages;

    /**
     * Every tafsir.
     *
     * @param locale the request's language (for the message)
     * @return {@code 200} with the tafsirs and the credit
     */
    @Operation(summary = "The tafsirs, in order")
    @GetMapping(path = "/tafsirs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse<DtoTafsirs>> getTafsirs(Locale locale) {
        return ok(locale, tafsirRetrievalService.getTafsirs());
    }

    /**
     * Every tafsir of an ayah.
     *
     * @param surahNumber the surah, 1 to 114
     * @param ayahNumber  the ayah
     * @param locale      the request's language (for the message)
     * @return {@code 200} with the texts
     */
    @Operation(summary = "Every tafsir's text for one ayah (a text may cover several ayahs)")
    @GetMapping(path = "/surahs/{surahNumber}/ayahs/{ayahNumber}/tafsirs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse<DtoAyahTafsirs>> getAyahTafsirs(@PathVariable @Min(1) @Max(114) int surahNumber,
                                                                       @PathVariable @Min(1) @Max(286) int ayahNumber,
                                                                       Locale locale) {
        return ok(locale, tafsirRetrievalService.getAyahTafsirs(surahNumber, ayahNumber, null));
    }

    /**
     * One tafsir of an ayah.
     *
     * @param tafsirId    the tafsir
     * @param surahNumber the surah, 1 to 114
     * @param ayahNumber  the ayah
     * @param locale      the request's language (for the message)
     * @return {@code 200} with the text (an empty list when the tafsir has none for this ayah)
     */
    @Operation(summary = "One tafsir's text for one ayah")
    @GetMapping(path = "/tafsirs/{tafsirId}/surahs/{surahNumber}/ayahs/{ayahNumber}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse<DtoAyahTafsirs>> getAyahTafsir(@PathVariable int tafsirId,
                                                                      @PathVariable @Min(1) @Max(114) int surahNumber,
                                                                      @PathVariable @Min(1) @Max(286) int ayahNumber,
                                                                      Locale locale) {
        return ok(locale, tafsirRetrievalService.getAyahTafsirs(surahNumber, ayahNumber, tafsirId));
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
