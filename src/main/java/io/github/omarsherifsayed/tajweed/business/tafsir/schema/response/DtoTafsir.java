package io.github.omarsherifsayed.tajweed.business.tafsir.schema.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A tafsir.
 *
 * @param id       the provider's id
 * @param nameAr   its name in Arabic as the provider gives it, absent when it gives none
 * @param nameEn   its name in English as the provider gives it
 * @param authorAr the author as the provider's Arabic list gives it, absent when none
 * @param authorEn the author as the provider's English list gives it, absent when none
 * @param language the language of its text
 */
@Schema(description = "A tafsir: names, author, language.")
public record DtoTafsir(int id, String nameAr, String nameEn, String authorAr, String authorEn, String language) {
}
