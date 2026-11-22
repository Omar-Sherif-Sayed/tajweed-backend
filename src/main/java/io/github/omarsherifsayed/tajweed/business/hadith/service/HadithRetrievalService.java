package io.github.omarsherifsayed.tajweed.business.hadith.service;

import io.github.omarsherifsayed.tajweed.business.hadith.schema.response.DtoHadith;
import io.github.omarsherifsayed.tajweed.business.hadith.schema.response.DtoHadithCategories;
import io.github.omarsherifsayed.tajweed.business.hadith.schema.response.DtoHadithPage;
import java.util.Locale;

/**
 * Reads HadeethEnc's hadiths as the sync stored them.
 */
public interface HadithRetrievalService {

    /**
     * Every category, titled in the request's language (or Arabic).
     *
     * @param locale the request's language
     * @return the categories
     */
    DtoHadithCategories getCategories(Locale locale);

    /**
     * One page of the hadiths of a category and its sub-categories.
     *
     * @param categoryId the category
     * @param page       the page, from 0
     * @param size       the page's size
     * @param locale     the request's language
     * @return the page
     */
    DtoHadithPage getHadiths(int categoryId, int page, int size, Locale locale);

    /**
     * One hadith.
     *
     * @param hadithId the hadith
     * @param locale   the request's language
     * @return the hadith in Arabic, and in the request's language when it exists
     */
    DtoHadith getHadith(int hadithId, Locale locale);
}
