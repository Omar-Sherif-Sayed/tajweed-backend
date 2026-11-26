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
 * {@code /v1/pages/{page}} — a page of the Madinah mushaf (1 – 604; outside it answers {@code 400 INVALID_REQUEST}):
 * its ayahs and its lines as printed (1405H).
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
     * A page: its ayahs and its printed lines.
     *
     * @param page   the page, 1 to 604
     * @param locale the request's language (for the message)
     * @return {@code 200} with the ayahs, the lines and their sources
     */
    @Operation(summary = "A page of the Madinah mushaf (1 – 604): its ayahs and its lines as printed (1405H)")
    @GetMapping(path = "/{page}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse<DtoPage>> getPage(@PathVariable @Min(1) @Max(604) int page, Locale locale) {
        return ResponseEntity.ok()
                .cacheControl(HttpCaching.CONTENT)
                .varyBy(HttpHeaders.ACCEPT_LANGUAGE)
                .body(BaseResponse.ok(messages.get(locale, "response.success"), pageRetrievalService.getPage(page)));
    }
}
