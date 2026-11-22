package io.github.omarsherifsayed.tajweed.business.tafsir.schema.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One tafsir's text for an ayah. The text may cover several ayahs: {@code from} … {@code to}.
 *
 * @param tafsir the tafsir
 * @param from   the first ayah the text covers ({@code surah:ayah})
 * @param to     the last ayah it covers
 * @param text   the text exactly as received (HTML) — the clients show it, never edit it
 */
@Schema(description = "One tafsir's text for an ayah (it may cover a range), as received.")
public record DtoAyahTafsir(DtoTafsir tafsir, String from, String to, String text) {
}
