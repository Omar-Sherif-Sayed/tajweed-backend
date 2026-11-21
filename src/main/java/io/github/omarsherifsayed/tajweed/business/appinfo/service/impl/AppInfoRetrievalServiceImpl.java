package io.github.omarsherifsayed.tajweed.business.appinfo.service.impl;

import io.github.omarsherifsayed.tajweed.business.appinfo.mapper.AppInfoMapper;
import io.github.omarsherifsayed.tajweed.business.appinfo.schema.response.DtoAppInfo;
import io.github.omarsherifsayed.tajweed.business.appinfo.service.AppInfoRetrievalService;
import io.github.omarsherifsayed.tajweed.general.config.AppProperties;
import io.github.omarsherifsayed.tajweed.general.i18n.MessageService;
import io.github.omarsherifsayed.tajweed.general.i18n.SupportedLanguages;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Builds the app-info answer from the message files (the names, in each language) and the application's settings
 * (the API version, the author's link). Nothing is read from the database.
 */
@Service
@RequiredArgsConstructor
public class AppInfoRetrievalServiceImpl implements AppInfoRetrievalService {

    /** Reads the names in the requested language. */
    private final MessageService messages;

    /** The application's settings. */
    private final AppProperties properties;

    /**
     * {@inheritDoc}
     *
     * @param locale the language of the answer
     * @return the facts, with texts in that language
     */
    @Override
    public DtoAppInfo getAppInfo(Locale locale) {
        return AppInfoMapper.toDto(
                messages.get(locale, "app.name"),
                messages.get(locale, "app.short_name"),
                messages.get(locale, "app.author"),
                properties.app().authorLink(),
                properties.app().apiVersion(),
                locale.getLanguage(),
                SupportedLanguages.CODES);
    }
}
