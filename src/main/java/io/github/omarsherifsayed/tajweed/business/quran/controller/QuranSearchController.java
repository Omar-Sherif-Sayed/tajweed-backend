package io.github.omarsherifsayed.tajweed.business.quran.controller;

import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoQuranSearch;
import io.github.omarsherifsayed.tajweed.business.quran.service.QuranSearchService;
import io.github.omarsherifsayed.tajweed.general.config.HttpCaching;
import io.github.omarsherifsayed.tajweed.general.i18n.MessageService;
import io.github.omarsherifsayed.tajweed.general.schema.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /v1/quran-search} — one search box, three kinds of search ({@code AGENTS.md} §8.1, F16): words with or
 * without tashkeel, a surah name, an ayah reference.
 *
 * <p>An empty or too long query, or one with nothing left once tidied, answers {@code 400 INVALID_REQUEST}. The
 * answer depends only on the query and the content, so it may be cached for a day ({@link HttpCaching#CONTENT}).
 */
@Tag(name = "Quran", description = "Surahs, ayahs, pages and juz of the Madinah mushaf — text verbatim from Tanzil.")
@RestController
@RequestMapping("/v1/quran-search")
@RequiredArgsConstructor
public class QuranSearchController {

    /** Searches. */
    private final QuranSearchService quranSearchService;

    /** Reads the success message in the request's language. */
    private final MessageService messages;

    /**
     * Searches the Quran.
     *
     * @param q      the query: words (with or without tashkeel), a surah name, or a reference such as {@code 2:255}
     * @param surah  the surah open in the reader, so that a number alone names one of its ayahs
     * @param page   the page of word results, from 0
     * @param size   word results per page, 1 to 100
     * @param locale the request's language (for the message)
     * @return {@code 200} with the named ayah, the named surahs and the ayahs that contain the words
     */
    @Operation(summary = "Search the Quran: words with or without tashkeel, a surah name, or an ayah reference")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse<DtoQuranSearch>> search(
            @Parameter(description = "Words, a surah name, or a reference such as 2:255")
            @RequestParam @NotBlank @Size(max = 100) String q,
            @Parameter(description = "The surah open in the reader (a number alone then names one of its ayahs)")
            @RequestParam(required = false) @Min(1) @Max(114) Integer surah,
            @RequestParam(defaultValue = "0") @Min(0) @Max(6236) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Locale locale) {
        return ResponseEntity.ok()
                .cacheControl(HttpCaching.CONTENT)
                .varyBy(HttpHeaders.ACCEPT_LANGUAGE)
                .body(BaseResponse.ok(messages.get(locale, "response.success"),
                        quranSearchService.search(q, surah, page, size)));
    }
}
