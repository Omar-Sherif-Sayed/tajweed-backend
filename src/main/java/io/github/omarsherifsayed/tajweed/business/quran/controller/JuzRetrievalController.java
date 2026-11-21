package io.github.omarsherifsayed.tajweed.business.quran.controller;

import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoJuz;
import io.github.omarsherifsayed.tajweed.business.quran.service.JuzRetrievalService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /v1/juzs} — where each of the 30 juz starts, for the index of the mushaf.
 */
@Tag(name = "Quran")
@RestController
@RequestMapping("/v1/juzs")
@RequiredArgsConstructor
public class JuzRetrievalController {

    /** Builds the answer. */
    private final JuzRetrievalService juzRetrievalService;

    /** Reads the success message in the request's language. */
    private final MessageService messages;

    /**
     * Where each juz starts.
     *
     * @param locale the request's language (for the message)
     * @return {@code 200} with the 30 juz in order
     */
    @Operation(summary = "The 30 juz: the first ayah and page of each")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse<List<DtoJuz>>> getJuzs(Locale locale) {
        return ResponseEntity.ok()
                .cacheControl(HttpCaching.CONTENT)
                .varyBy(HttpHeaders.ACCEPT_LANGUAGE)
                .body(BaseResponse.ok(messages.get(locale, "response.success"), juzRetrievalService.getJuzs()));
    }
}
