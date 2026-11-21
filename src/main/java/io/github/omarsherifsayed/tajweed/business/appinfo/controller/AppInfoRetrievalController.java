package io.github.omarsherifsayed.tajweed.business.appinfo.controller;

import io.github.omarsherifsayed.tajweed.business.appinfo.schema.response.DtoAppInfo;
import io.github.omarsherifsayed.tajweed.business.appinfo.service.AppInfoRetrievalService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /v1/app-info} — the application's name, author, API version and languages.
 *
 * <p>The model of every controller ({@code AGENTS.md} §9): the path, the status and the caching headers only; the
 * work is done by the service. The language comes from {@code Accept-Language} (Spring passes it as {@link Locale}).
 */
@Tag(name = "App info", description = "The application's name, author, API version and languages.")
@RestController
@RequestMapping("/v1/app-info")
@RequiredArgsConstructor
public class AppInfoRetrievalController {

    /** Builds the answer. */
    private final AppInfoRetrievalService appInfoRetrievalService;

    /** Reads the success message in the request's language. */
    private final MessageService messages;

    /**
     * Returns the facts about the application in the request's language.
     *
     * @param locale the request's language, from {@code Accept-Language} (Arabic by default)
     * @return {@code 200} with the facts, cacheable for an hour, varying with the language
     */
    @Operation(summary = "The application's name, author, API version and languages")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse<DtoAppInfo>> getAppInfo(Locale locale) {
        DtoAppInfo info = appInfoRetrievalService.getAppInfo(locale);
        return ResponseEntity.ok()
                .cacheControl(HttpCaching.APP_INFO)
                .varyBy(HttpHeaders.ACCEPT_LANGUAGE)
                .body(BaseResponse.ok(messages.get(locale, "response.success"), info));
    }
}
