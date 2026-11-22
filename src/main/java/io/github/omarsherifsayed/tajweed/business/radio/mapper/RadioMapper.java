package io.github.omarsherifsayed.tajweed.business.radio.mapper;

import io.github.omarsherifsayed.tajweed.business.radio.entity.RadioStation;
import io.github.omarsherifsayed.tajweed.business.radio.entity.RadioStream;
import io.github.omarsherifsayed.tajweed.business.radio.entity.RadioStreamCheck;
import io.github.omarsherifsayed.tajweed.business.radio.schema.response.DtoRadioStation;
import io.github.omarsherifsayed.tajweed.business.radio.schema.response.DtoRadioStream;
import io.github.omarsherifsayed.tajweed.business.reciter.schema.response.DtoRiwayah;
import java.util.List;

/**
 * Builds the radio module's response objects ({@code AGENTS.md} §9: {@code final}, static methods).
 */
public final class RadioMapper {

    /**
     * Not instantiated: static methods only.
     */
    private RadioMapper() {
    }

    /**
     * A station.
     *
     * @param station the row
     * @param riwayah its riwayah, or {@code null}
     * @param streams its streams, already built
     * @return the response object
     */
    public static DtoRadioStation toDto(RadioStation station, DtoRiwayah riwayah, List<DtoRadioStream> streams) {
        return new DtoRadioStation(station.getId(), station.getKind(), station.getNameAr(), station.getNameEn(),
                station.getReciterId() == null ? null : station.getReciterId().intValue(), riwayah,
                station.getCountryCode(), station.getBroadcasterUrl(), streams);
    }

    /**
     * A stream with its last health check.
     *
     * @param stream the row
     * @param check  its health, or {@code null} before the first check
     * @return the response object
     */
    public static DtoRadioStream toDto(RadioStream stream, RadioStreamCheck check) {
        return new DtoRadioStream(stream.getPriority(), stream.getUrl(), check == null ? null : check.getStatus(),
                check == null ? null : check.getLastCheckedAt());
    }
}
