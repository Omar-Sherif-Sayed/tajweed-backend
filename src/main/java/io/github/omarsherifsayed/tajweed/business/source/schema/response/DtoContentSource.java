package io.github.omarsherifsayed.tajweed.business.source.schema.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * A source of the content, as the clients show it in their credits and Sources section (Tanzil's licence requires the
 * source to be shown with a link — {@code AGENTS.md} §7.1).
 *
 * @param code       a stable name, for example {@code TANZIL_UTHMANI}
 * @param name       the source's own name and version
 * @param licence    its licence
 * @param sourceUrl  where it comes from (to be shown as a link)
 * @param sha256     the SHA-256 of the file we use
 * @param obtainedOn the day it was obtained
 */
@Schema(description = "A source of the content: name, licence, link and checksum.")
public record DtoContentSource(String code, String name, String licence, String sourceUrl, String sha256,
                               LocalDate obtainedOn) {
}
