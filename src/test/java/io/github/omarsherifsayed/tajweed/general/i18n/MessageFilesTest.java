package io.github.omarsherifsayed.tajweed.general.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.omarsherifsayed.tajweed.general.exception.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Proves that the message files of the 22 languages are complete: every file exists, holds exactly the keys of the
 * Arabic file, has no empty text, and every {@link ErrorCode}'s key is present ({@code AGENTS.md} §1: every
 * user-facing text in all 22 languages).
 */
class MessageFilesTest {

    /**
     * The 22 language codes, for the parameterised test.
     *
     * @return the codes of {@link SupportedLanguages}
     */
    static List<String> codes() {
        return SupportedLanguages.CODES;
    }

    @ParameterizedTest(name = "messages file of {0}")
    @MethodSource("codes")
    @DisplayName("Every language's file holds exactly the Arabic file's keys, none empty")
    void everyLanguageIsComplete(String code) throws IOException {
        // given
        Properties arabic = load("ar");

        // when
        Properties other = load(code);

        // then
        assertThat(other.stringPropertyNames()).isEqualTo(arabic.stringPropertyNames());
        other.stringPropertyNames().forEach(key -> assertThat(other.getProperty(key)).as(code + " " + key).isNotBlank());
    }

    @Test
    @DisplayName("Every error code has its message key")
    void everyErrorCodeHasAMessage() throws IOException {
        Set<String> keys = load("ar").stringPropertyNames();
        Arrays.stream(ErrorCode.values()).forEach(code -> assertThat(keys).contains(code.getMessageKey()));
    }

    /**
     * Reads one language's message file from the class path.
     *
     * @param code the language code ({@code ar} is {@code messages.properties})
     * @return its keys and texts
     * @throws IOException if the file cannot be read
     */
    private static Properties load(String code) throws IOException {
        String name = "ar".equals(code) ? "i18n/messages.properties" : "i18n/messages_" + code + ".properties";
        try (InputStream in = MessageFilesTest.class.getClassLoader().getResourceAsStream(name)) {
            assertThat(in).as("missing file " + name).isNotNull();
            Properties properties = new Properties();
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return properties;
        }
    }
}
