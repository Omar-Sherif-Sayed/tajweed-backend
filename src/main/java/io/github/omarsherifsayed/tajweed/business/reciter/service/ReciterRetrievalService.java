package io.github.omarsherifsayed.tajweed.business.reciter.service;

import io.github.omarsherifsayed.tajweed.business.reciter.schema.response.DtoRecitation;
import io.github.omarsherifsayed.tajweed.business.reciter.schema.response.DtoReciter;
import io.github.omarsherifsayed.tajweed.business.reciter.schema.response.DtoSurahTimings;
import java.util.List;

/**
 * Reads the reciters, their recitations, the audio sources and the ayah timings.
 */
public interface ReciterRetrievalService {

    /**
     * Every reciter with his recitations.
     *
     * @return the reciters, in the order of the catalogue
     */
    List<DtoReciter> getReciters();

    /**
     * One reciter with his recitations.
     *
     * @param reciterId the reciter
     * @return the reciter
     * @throws io.github.omarsherifsayed.tajweed.general.exception.BaseException {@code NOT_FOUND_RECITER}
     */
    DtoReciter getReciter(int reciterId);

    /**
     * One recitation with its surahs, its sources in order and its credits.
     *
     * @param recitationId the recitation
     * @return the recitation
     * @throws io.github.omarsherifsayed.tajweed.general.exception.BaseException {@code NOT_FOUND_RECITATION}
     */
    DtoRecitation getRecitation(int recitationId);

    /**
     * The ayah timings of one surah of a recitation.
     *
     * @param recitationId the recitation
     * @param surahNumber  the surah, 1 to 114 (checked by the controller)
     * @return the timings
     * @throws io.github.omarsherifsayed.tajweed.general.exception.BaseException {@code NOT_FOUND_RECITATION}, or
     *                                                                           {@code NOT_FOUND_TIMINGS} when it has none
     *                                                                           for this surah
     */
    DtoSurahTimings getTimings(int recitationId, int surahNumber);
}
