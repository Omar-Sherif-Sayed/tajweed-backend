package io.github.omarsherifsayed.tajweed.business.reciter.mapper;

import io.github.omarsherifsayed.tajweed.business.reciter.entity.AudioSource;
import io.github.omarsherifsayed.tajweed.business.reciter.entity.AudioSourceCheck;
import io.github.omarsherifsayed.tajweed.business.reciter.entity.AyahTiming;
import io.github.omarsherifsayed.tajweed.business.reciter.entity.Recitation;
import io.github.omarsherifsayed.tajweed.business.reciter.entity.Riwayah;
import io.github.omarsherifsayed.tajweed.business.reciter.schema.response.DtoAudioSource;
import io.github.omarsherifsayed.tajweed.business.reciter.schema.response.DtoAyahTiming;
import io.github.omarsherifsayed.tajweed.business.reciter.schema.response.DtoRecitationSummary;
import io.github.omarsherifsayed.tajweed.business.reciter.schema.response.DtoRiwayah;

/**
 * Builds the reciter module's response objects ({@code AGENTS.md} §9: {@code final}, static methods).
 */
public final class ReciterMapper {

    /** The number of surahs of a complete recitation. */
    private static final int SURAHS = 114;

    /**
     * Not instantiated: static methods only.
     */
    private ReciterMapper() {
    }

    /**
     * A riwayah.
     *
     * @param riwayah the row
     * @return the response object
     */
    public static DtoRiwayah toDto(Riwayah riwayah) {
        return new DtoRiwayah(riwayah.getId(), riwayah.getNameAr(), riwayah.getNameEn(), riwayah.getRawiAr(),
                riwayah.getQariAr(), riwayah.getMatchesDisplayedText());
    }

    /**
     * A recitation as a list shows it.
     *
     * @param recitation the row
     * @param riwayah    its riwayah
     * @return the response object; listening only when the riwayah is not the one of the displayed text
     */
    public static DtoRecitationSummary toDto(Recitation recitation, Riwayah riwayah) {
        return new DtoRecitationSummary(recitation.getId(), recitation.getReciterId(), recitation.getNameAr(),
                recitation.getNameEn(), recitation.getStyle(), toDto(riwayah), recitation.getProvider(),
                !riwayah.getMatchesDisplayedText(),
                recitation.getSurahCount(), recitation.getSurahCount() == SURAHS, recitation.getHasTimings());
    }

    /**
     * An audio source with its last health check.
     *
     * @param source the row
     * @param check  its health, or {@code null} before the first check
     * @return the response object
     */
    public static DtoAudioSource toDto(AudioSource source, AudioSourceCheck check) {
        return new DtoAudioSource(source.getPriority(), source.getKind(), source.getProvider(), source.getUrlTemplate(),
                source.getBitrateKbps() == null ? null : source.getBitrateKbps().intValue(), source.getDownloadAllowed(),
                check == null ? null : check.getStatus(), check == null ? null : check.getLastCheckedAt());
    }

    /**
     * One ayah's timing.
     *
     * @param timing the row
     * @return the response object
     */
    public static DtoAyahTiming toDto(AyahTiming timing) {
        return new DtoAyahTiming(timing.getAyahNumber(), timing.getStartMs(), timing.getEndMs());
    }
}
