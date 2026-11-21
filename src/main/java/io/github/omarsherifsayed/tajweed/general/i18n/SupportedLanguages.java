package io.github.omarsherifsayed.tajweed.general.i18n;

import java.util.List;
import java.util.Locale;

/**
 * The 22 languages of the product ({@code AGENTS.md} §2, §8.9), in the order of the product's language table.
 *
 * <p>Arabic is the default: a request without {@code Accept-Language}, or asking only for languages not in this
 * list, is answered in Arabic. Every language has its own {@code i18n/messages_<code>.properties} file (Arabic is
 * {@code messages.properties}), and a test checks that all of them hold the same keys.
 */
public final class SupportedLanguages {

    /** The default language: Arabic. */
    public static final Locale DEFAULT = Locale.of("ar");

    /** The language codes, as used in the message file names and in {@code Accept-Language}. */
    public static final List<String> CODES = List.of(
            "ar", "en", "ru", "es", "de", "it", "fr", "ur", "hi", "ko", "id",
            "ja", "zh", "tr", "bn", "fa", "ms", "sw", "ha", "pt", "ps", "uz");

    /** The same languages as {@link Locale} objects. */
    public static final List<Locale> LOCALES = CODES.stream().map(Locale::of).toList();

    /**
     * Not instantiated: this class only holds constants.
     */
    private SupportedLanguages() {
    }
}
