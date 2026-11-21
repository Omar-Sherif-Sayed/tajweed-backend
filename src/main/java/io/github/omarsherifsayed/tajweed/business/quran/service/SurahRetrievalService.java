package io.github.omarsherifsayed.tajweed.business.quran.service;

import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoAyah;
import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoSurah;
import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoSurahAyahs;
import java.util.List;

/**
 * Reads the surahs and their ayahs.
 */
public interface SurahRetrievalService {

    /**
     * The 114 surahs, for the index of the mushaf.
     *
     * @return every surah in order, with the page where it starts
     */
    List<DtoSurah> getSurahs();

    /**
     * A surah with all its ayahs.
     *
     * @param surahNumber the surah, 1 to 114 (checked by the controller)
     * @return the surah, its ayahs and the source of the text
     */
    DtoSurahAyahs getSurahAyahs(int surahNumber);

    /**
     * One ayah.
     *
     * @param surahNumber the surah, 1 to 114
     * @param ayahNumber  the ayah's number in it
     * @return the ayah
     * @throws io.github.omarsherifsayed.tajweed.general.exception.BaseException with {@code NOT_FOUND_AYAH} when the
     *         surah has no ayah with that number
     */
    DtoAyah getAyah(int surahNumber, int ayahNumber);
}
