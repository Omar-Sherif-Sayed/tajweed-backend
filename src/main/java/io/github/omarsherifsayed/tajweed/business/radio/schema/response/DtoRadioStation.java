package io.github.omarsherifsayed.tajweed.business.radio.schema.response;

import io.github.omarsherifsayed.tajweed.business.radio.enums.RadioKind;
import io.github.omarsherifsayed.tajweed.business.reciter.schema.response.DtoRiwayah;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * A radio station.
 *
 * @param id             our id
 * @param kind           what it broadcasts
 * @param nameAr         its name in Arabic, as the source writes it
 * @param nameEn         its name in English, as the source writes it
 * @param reciterId      our reciter ({@code /v1/reciters/{id}}) when it is one of them; absent otherwise
 * @param riwayah        the riwayah, only when the channel names it; absent otherwise (the source does not say)
 * @param countryCode    the country of a national station; absent otherwise
 * @param broadcasterUrl the broadcaster's own page, for a national station; absent otherwise
 * @param streams        its streams, in the order to try them
 */
@Schema(description = "A radio station: names, kind, reciter and riwayah when known, streams in order with their health.")
public record DtoRadioStation(int id, RadioKind kind, String nameAr, String nameEn, Integer reciterId, DtoRiwayah riwayah,
                              String countryCode, String broadcasterUrl, List<DtoRadioStream> streams) {
}
