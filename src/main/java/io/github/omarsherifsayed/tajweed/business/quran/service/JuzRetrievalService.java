package io.github.omarsherifsayed.tajweed.business.quran.service;

import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoJuz;
import java.util.List;

/**
 * Reads where each juz starts.
 */
public interface JuzRetrievalService {

    /**
     * The 30 juz, for the index of the mushaf.
     *
     * @return where each juz starts, in order
     */
    List<DtoJuz> getJuzs();
}
