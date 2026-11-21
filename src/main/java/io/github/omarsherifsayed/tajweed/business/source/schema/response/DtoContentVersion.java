package io.github.omarsherifsayed.tajweed.business.source.schema.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * The version of the content: the apps keep it, and refresh their copies when it changes.
 *
 * @param version a short fingerprint of all the sources' checksums — it changes whenever a source file changes
 * @param sources the sources the content is made from
 */
@Schema(description = "The content's version (a fingerprint of its sources) and the sources themselves.")
public record DtoContentVersion(String version, List<DtoContentSource> sources) {
}
