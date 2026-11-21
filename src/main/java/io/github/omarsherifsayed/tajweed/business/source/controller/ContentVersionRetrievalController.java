package io.github.omarsherifsayed.tajweed.business.source.controller;

import io.github.omarsherifsayed.tajweed.business.source.schema.response.DtoContentVersion;
import io.github.omarsherifsayed.tajweed.business.source.service.ContentSourceRetrievalService;
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
 * {@code GET /v1/content-version} — the content's version and its sources. The apps keep the version and download
 * fresh content only when it changes ({@code AGENTS.md} §5).
 */
@Tag(name = "Content", description = "The content's version and its sources.")
@RestController
@RequestMapping("/v1/content-version")
@RequiredArgsConstructor
public class ContentVersionRetrievalController {

    /** Builds the answer. */
    private final ContentSourceRetrievalService contentSourceRetrievalService;

    /** Reads the success message in the request's language. */
    private final MessageService messages;

    /**
     * Returns the content's version and its sources.
     *
     * @param locale the request's language (for the message)
     * @return {@code 200} with the version and the sources
     */
    @Operation(summary = "The content's version (a fingerprint of its sources) and the sources")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse<DtoContentVersion>> getContentVersion(Locale locale) {
        return ResponseEntity.ok()
                .cacheControl(HttpCaching.CONTENT)
                .varyBy(HttpHeaders.ACCEPT_LANGUAGE)
                .body(BaseResponse.ok(messages.get(locale, "response.success"),
                        contentSourceRetrievalService.getContentVersion()));
    }
}
