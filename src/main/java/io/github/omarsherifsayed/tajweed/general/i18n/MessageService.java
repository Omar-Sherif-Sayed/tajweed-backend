package io.github.omarsherifsayed.tajweed.general.i18n;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

/**
 * Reads a text from the message files ({@code i18n/messages_*.properties}) in the request's language.
 *
 * <p>Every user-facing text of the API comes from these files, never from the code ({@code AGENTS.md} §8.9).
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    /** Spring's message source, configured by {@code spring.messages.*} in {@code application.properties}. */
    private final MessageSource messageSource;

    /**
     * Returns a text in the language of the current request.
     *
     * @param key  the message key, for example {@code error.not_found_path}
     * @param args values for the {@code {0}}, {@code {1}} … slots of the text, if it has any
     * @return the text in the current language (Arabic when the request asked for none)
     */
    public String get(String key, Object... args) {
        return get(LocaleContextHolder.getLocale(), key, args);
    }

    /**
     * Returns a text in a given language.
     *
     * @param locale the language
     * @param key    the message key
     * @param args   values for the slots of the text, if it has any
     * @return the text in that language
     */
    public String get(Locale locale, String key, Object... args) {
        return messageSource.getMessage(key, args, locale);
    }
}
