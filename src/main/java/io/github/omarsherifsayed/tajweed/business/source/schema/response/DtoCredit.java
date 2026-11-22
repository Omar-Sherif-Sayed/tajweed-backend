package io.github.omarsherifsayed.tajweed.business.source.schema.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The credit of content fetched at run time from its owner, shown wherever that content is shown.
 *
 * @param name  the owner's name as it asks to be credited
 * @param url   the owner's address, shown as a link
 * @param terms the terms we use it under, in a few words
 */
@Schema(description = "The credit of content fetched from its owner: name, link and terms.")
public record DtoCredit(String name, String url, String terms) {
}
