package io.github.omarsherifsayed.tajweed.business.radio.schema.response;

import io.github.omarsherifsayed.tajweed.business.source.schema.response.DtoContentSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Radio stations with the source to credit.
 *
 * @param stations the stations, in the list's order
 * @param credits  who lists them — shown on the radio screen
 */
@Schema(description = "Radio stations and their credits.")
public record DtoRadios(List<DtoRadioStation> stations, List<DtoContentSource> credits) {
}
