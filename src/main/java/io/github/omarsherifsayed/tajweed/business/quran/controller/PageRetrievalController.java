package io.github.omarsherifsayed.tajweed.business.quran.controller;

import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoPage;
import io.github.omarsherifsayed.tajweed.business.quran.service.PageRetrievalService;
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
 * {@code /v1/pages/{page}} — the ayahs that start on a page of the Madinah mushaf (1 – 604; outside it answers
 * {@code 400 INVALID_REQUEST}).
 */
@Tag(name = "Quran")
@RestController
@RequestMapping("/v1/pages")
@RequiredArgsConstructor
public class PageRetrievalController {

    /** Builds the answer. */
    private final PageRetrievalService pageRetrievalService;

    /** Reads the success message in the request's language. */
    private final MessageService messages;

    /**
     * The ayahs that start on a page.
     *
     * @param page   the page, 1 to 604
     * @param locale the request's language (for the message)
     * @return {@code 200} with the ayahs and the source of the text
     */
    @Operation(summary = "The ayahs that start on a page of the Madinah mushaf (1 – 604)")
    @GetMapping(path = "/{page}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse<DtoPage>> getPage(@PathVariable @Min(1) @Max(604) int page, Locale locale) {
        return ResponseEntity.ok()
                .cacheControl(HttpCaching.CONTENT)
                .varyBy(HttpHeaders.ACCEPT_LANGUAGE)
                .body(BaseResponse.ok(messages.get(locale, "response.success"), pageRetrievalService.getPage(page)));
    }
}
