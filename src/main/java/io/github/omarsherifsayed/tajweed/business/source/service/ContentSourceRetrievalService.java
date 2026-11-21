package io.github.omarsherifsayed.tajweed.business.source.service;

import io.github.omarsherifsayed.tajweed.business.source.schema.response.DtoContentSource;
import io.github.omarsherifsayed.tajweed.business.source.schema.response.DtoContentVersion;

/**
 * Reads the content sources and the content's version.
 */
public interface ContentSourceRetrievalService {

    /** The code of the Quran text's source (Tanzil Uthmani), credited beside every Quran answer. */
    String QURAN_TEXT = "TANZIL_UTHMANI";

    /**
     * The content's version and its sources.
     *
     * @return the fingerprint of all sources, and the sources
     */
    DtoContentVersion getContentVersion();

    /**
     * One source by its code.
     *
     * @param code the source's stable code
     * @return the source
     * @throws IllegalStateException if it was never imported (a broken database, not a client error)
     */
    DtoContentSource getSource(String code);
}
