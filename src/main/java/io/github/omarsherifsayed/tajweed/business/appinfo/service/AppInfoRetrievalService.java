package io.github.omarsherifsayed.tajweed.business.appinfo.service;

import io.github.omarsherifsayed.tajweed.business.appinfo.schema.response.DtoAppInfo;
import java.util.Locale;

/**
 * Reads the facts about the application: its name, its author, the API version and the languages.
 */
public interface AppInfoRetrievalService {

    /**
     * Returns the application's facts in a language.
     *
     * @param locale the language of the answer (already limited to the 22 supported languages)
     * @return the facts, with texts in that language
     */
    DtoAppInfo getAppInfo(Locale locale);
}
