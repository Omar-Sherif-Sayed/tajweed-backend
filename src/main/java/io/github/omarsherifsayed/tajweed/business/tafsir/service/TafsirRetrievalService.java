package io.github.omarsherifsayed.tajweed.business.tafsir.service;

import io.github.omarsherifsayed.tajweed.business.tafsir.schema.response.DtoAyahTafsirs;
import io.github.omarsherifsayed.tajweed.business.tafsir.schema.response.DtoTafsirs;

/**
 * Reads the synced tafsirs.
 */
public interface TafsirRetrievalService {

    /**
     * Every tafsir.
     *
     * @return the tafsirs, with the credit
     */
    DtoTafsirs getTafsirs();

    /**
     * Every tafsir's text for one ayah — or one tafsir's, when {@code tafsirId} is given.
     *
     * @param surahNumber the surah
     * @param ayahNumber  the ayah
     * @param tafsirId    one tafsir, or {@code null} for all
     * @return the texts
     */
    DtoAyahTafsirs getAyahTafsirs(int surahNumber, int ayahNumber, Integer tafsirId);
}
