package io.github.omarsherifsayed.tajweed.business.reciter.controller;

import io.github.omarsherifsayed.tajweed.business.reciter.schema.response.DtoReciter;
import io.github.omarsherifsayed.tajweed.business.reciter.service.ReciterRetrievalService;
import io.github.omarsherifsayed.tajweed.general.config.HttpCaching;
import io.github.omarsherifsayed.tajweed.general.i18n.MessageService;
import io.github.omarsherifsayed.tajweed.general.schema.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * {@code /v1/reciters} — the reciters and their recitations, each with its riwayah said plainly.
 *
 * <p>An unknown id answers {@code 404 NOT_FOUND_RECITER}. The catalogue changes only with a migration, so the answers
 * may be cached for a day ({@link HttpCaching#CONTENT}).
 */
@Tag(name = "Reciters", description = "Reciters, recitations (riwayah, style, surahs), audio sources and ayah timings.")
@RestController
@RequestMapping("/v1/reciters")
@RequiredArgsConstructor
public class ReciterRetrievalController {

    /** Builds the answers. */
    private final ReciterRetrievalService reciterRetrievalService;

    /** Reads the success message in the request's language. */
    private final MessageService messages;

    /**
     * Every reciter with his recitations.
     *
     * @param locale the request's language (for the message)
     * @return {@code 200} with the reciters
     */
    @Operation(summary = "The reciters, each with his recitations: riwayah, style, surahs, timings")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse<List<DtoReciter>>> getReciters(Locale locale) {
        return ok(locale, reciterRetrievalService.getReciters());
    }

    /**
     * One reciter with his recitations.
     *
     * @param reciterId the reciter
     * @param locale    the request's language (for the message)
     * @return {@code 200} with the reciter
     */
    @Operation(summary = "One reciter with his recitations")
    @GetMapping(path = "/{reciterId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse<DtoReciter>> getReciter(@PathVariable int reciterId, Locale locale) {
        return ok(locale, reciterRetrievalService.getReciter(reciterId));
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
