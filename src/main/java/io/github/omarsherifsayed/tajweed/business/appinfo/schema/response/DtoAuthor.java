package io.github.omarsherifsayed.tajweed.business.appinfo.schema.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The application's author, as the About page shows him ({@code AGENTS.md} §2: one link only).
 *
 * @param name the author's name in the request's language: «عمر شريف» in Arabic, "Omar Sherif" otherwise
 * @param link his one public link (LinkedIn)
 */
@Schema(description = "The application's author and his one public link.")
public record DtoAuthor(String name, String link) {
}
