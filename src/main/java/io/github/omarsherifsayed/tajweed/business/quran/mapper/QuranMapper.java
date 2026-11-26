package io.github.omarsherifsayed.tajweed.business.quran.mapper;

import io.github.omarsherifsayed.tajweed.business.quran.entity.Ayah;
import io.github.omarsherifsayed.tajweed.business.quran.entity.MushafLine;
import io.github.omarsherifsayed.tajweed.business.quran.entity.Surah;
import io.github.omarsherifsayed.tajweed.business.quran.repository.JuzStartProjection;
import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoAyah;
import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoJuz;
import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoMushafLine;
import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoSurah;

/**
 * Builds the quran module's response objects ({@code AGENTS.md} §9: {@code final}, static methods). The text is
 * passed on exactly as stored.
 */
public final class QuranMapper {

    /** Quarters in a hizb. */
    private static final int QUARTERS_PER_HIZB = 4;

    /**
     * Not instantiated: static methods only.
     */
    private QuranMapper() {
    }

    /**
     * A surah as the clients see it.
     *
     * @param surah     the database row
     * @param startPage the page where it starts
     * @return the response object
     */
    public static DtoSurah toDto(Surah surah, int startPage) {
        return new DtoSurah(surah.getId(), surah.getNameAr(), surah.getNameTransliteration(), surah.getNameEn(),
                surah.getRevelationType(), surah.getRevelationOrder(), surah.getAyahCount(), startPage);
    }

    /**
     * An ayah as the clients see it; the hizb is derived from the quarter.
     *
     * @param ayah the database row
     * @return the response object, with the text unchanged
     */
    public static DtoAyah toDto(Ayah ayah) {
        int quarter = ayah.getHizbQuarter();
        return new DtoAyah(ayah.getId(), ayah.getSurahId(), ayah.getNumberInSurah(), ayah.getTextUthmani(),
                ayah.getBasmalaWordCount(), ayah.getPage(), ayah.getJuz(), (quarter - 1) / QUARTERS_PER_HIZB + 1,
                quarter, ayah.getSajdahType());
    }

    /**
     * A line of the printed mushaf as the clients see it.
     *
     * @param line the database row
     * @return the response object
     */
    public static DtoMushafLine toDto(MushafLine line) {
        return new DtoMushafLine(line.getLine(), line.getKind(),
                line.getSurahNumber() == null ? null : line.getSurahNumber().intValue(), line.getFirstAyahId(),
                line.getFirstWord() == null ? null : line.getFirstWord().intValue(), line.getLastAyahId(),
                line.getLastWord() == null ? null : line.getLastWord().intValue());
    }

    /**
     * Where a juz starts.
     *
     * @param start the query row
     * @return the response object
     */
    public static DtoJuz toDto(JuzStartProjection start) {
        return new DtoJuz(start.getJuz(), start.getSurahNumber(), start.getAyahNumber(), start.getPage());
    }
}
