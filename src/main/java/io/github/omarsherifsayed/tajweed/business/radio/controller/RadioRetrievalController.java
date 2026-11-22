package io.github.omarsherifsayed.tajweed.business.radio.controller;

import io.github.omarsherifsayed.tajweed.business.radio.schema.response.DtoRadios;
import io.github.omarsherifsayed.tajweed.business.radio.service.RadioRetrievalService;
import io.github.omarsherifsayed.tajweed.general.config.HttpCaching;
import io.github.omarsherifsayed.tajweed.general.i18n.MessageService;
import io.github.omarsherifsayed.tajweed.general.schema.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * {@code /v1/radios} — the Quran radio stations (F7) with their streams in order and their health.
 *
 * <p>An unknown id answers {@code 404 NOT_FOUND_RADIO}. The answers carry the streams' daily health, so they are
 * cached for an hour.
 */
@Tag(name = "Radio", description = "Quran radio stations: recitation only, streams in order with their health.")
@RestController
@RequestMapping("/v1/radios")
@RequiredArgsConstructor
public class RadioRetrievalController {

    /** Builds the answers. */
    private final RadioRetrievalService radioRetrievalService;

    /** Reads the success message in the request's language. */
    private final MessageService messages;

    /**
     * Every station.
     *
     * @param locale the request's language (for the message)
     * @return {@code 200} with the stations and the credits
     */
    @Operation(summary = "Every radio station, with its streams in order and their health")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse<DtoRadios>> getRadios(Locale locale) {
        return ok(locale, radioRetrievalService.getRadios());
    }

    /**
     * One station.
     *
     * @param stationId the station
     * @param locale    the request's language (for the message)
     * @return {@code 200} with the station and the credits
     */
    @Operation(summary = "One radio station")
    @GetMapping(path = "/{stationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse<DtoRadios>> getRadio(@PathVariable int stationId, Locale locale) {
        return ok(locale, radioRetrievalService.getRadio(stationId));
    }

    /**
     * A successful answer, cached for an hour.
     *
     * @param locale the request's language
     * @param data   the answer's data
     * @return {@code 200}, varying with the language
     */
    private ResponseEntity<BaseResponse<DtoRadios>> ok(Locale locale, DtoRadios data) {
        return ResponseEntity.ok()
                .cacheControl(HttpCaching.CHECKED_DAILY)
                .varyBy(HttpHeaders.ACCEPT_LANGUAGE)
                .body(BaseResponse.ok(messages.get(locale, "response.success"), data));
    }
}
