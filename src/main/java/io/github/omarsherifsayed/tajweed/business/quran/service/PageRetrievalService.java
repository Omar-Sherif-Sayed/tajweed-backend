package io.github.omarsherifsayed.tajweed.business.quran.service;

import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoPage;

/**
 * Reads the ayahs of a page of the Madinah mushaf.
 */
public interface PageRetrievalService {

    /**
     * The ayahs that start on a page.
     *
     * @param page the page, 1 to 604 (checked by the controller)
     * @return its ayahs and the source of the text
     */
    DtoPage getPage(int page);
}
