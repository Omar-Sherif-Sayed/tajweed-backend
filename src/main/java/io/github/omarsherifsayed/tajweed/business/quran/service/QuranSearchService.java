package io.github.omarsherifsayed.tajweed.business.quran.service;

import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoQuranSearch;

/**
 * Searches the Quran (F16): by words with or without tashkeel, by surah name, and by ayah reference.
 */
public interface QuranSearchService {

    /**
     * Searches.
     *
     * @param query     what the user typed, 1 to 100 characters (checked by the controller)
     * @param openSurah the surah open in the reader, so that a number alone names an ayah in it; {@code null} if none
     * @param page      the page of word results, from 0
     * @param size      word results per page, 1 to 100
     * @return the three kinds of result
     * @throws io.github.omarsherifsayed.tajweed.general.exception.BaseException {@code INVALID_REQUEST} when nothing
     *                                                                           is left of the query once tidied
     */
    DtoQuranSearch search(String query, Integer openSurah, int page, int size);
}
