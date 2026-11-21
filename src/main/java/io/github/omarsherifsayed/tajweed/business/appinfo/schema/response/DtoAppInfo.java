package io.github.omarsherifsayed.tajweed.business.appinfo.schema.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * What {@code GET /v1/app-info} returns: the application's name, its author, the API version and the languages.
 *
 * <p>The texts are in the request's language. The product's name exists in Arabic and English only ({@code AGENTS.md}
 * §2): Arabic answers carry the Arabic name, every other language the English name.
 *
 * @param name       the full name, «تجويد - القرآن الكريم وعلومه» or "Tajweed - The Noble Quran and Its Sciences"
 * @param shortName  the short name, «تجويد» or "Tajweed"
 * @param author     who made the application
 * @param apiVersion the version of the public API
 * @param language   the language this answer is written in (one of {@code languages})
 * @param languages  the codes of the 22 languages the API answers in, Arabic first
 */
@Schema(description = "The application's name, author, API version and languages.")
public record DtoAppInfo(String name, String shortName, DtoAuthor author, String apiVersion, String language,
                         List<String> languages) {
}
