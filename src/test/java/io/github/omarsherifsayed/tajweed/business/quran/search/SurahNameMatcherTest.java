package io.github.omarsherifsayed.tajweed.business.quran.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Proves the surah-name and ayah-reference search on Tanzil's real surah names (no database).
 */
class SurahNameMatcherTest {

    /** The matcher built from Tanzil's metadata. */
    private static SurahNameMatcher names;

    /**
     * Builds the matcher.
     */
    @BeforeAll
    static void build() {
        names = SearchTestFiles.names();
    }

    @Test
    @DisplayName("Al-Baqarah is found by its Arabic name, with or without «سورة» and «ال», and by Latin spellings")
    void findsASurahByName() {
        for (String q : new String[] {"البقرة", "سورة البقرة", "بقره", "Al-Baqara", "al baqarah", "Baqara", "The Cow",
            "surah al-baqarah", "2", "٢"}) {
            assertThat(names.match(q)).as(q).startsWith(2);
        }
    }

    @Test
    @DisplayName("Exact names come before names that only begin with the query")
    void putsExactMatchesFirst() {
        assertThat(names.match("ق")).containsExactly(50);
        assertThat(names.match("الرحمن")).startsWith(55);
        assertThat(names.match("آل عمران")).containsExactly(3);
        assertThat(names.match("Fatihah")).startsWith(1);
        assertThat(names.match("115")).isEmpty();
    }

    @Test
    @DisplayName("2:255, ٢:٢٥٥, «البقرة 255», «Al-Baqara 255» and, in an open surah, «255» all name the same ayah")
    void readsAReference() {
        for (String q : new String[] {"2:255", "2 255", "٢:٢٥٥", "البقرة 255", "سورة البقرة ٢٥٥", "Al-Baqara 255"}) {
            assertThat(names.reference(q, null)).as(q).hasValueSatisfying(r -> assertThat(r).containsExactly(2, 255));
        }
        assertThat(names.reference("255", 2)).hasValueSatisfying(r -> assertThat(r).containsExactly(2, 255));
        assertThat(names.reference("255", null)).isEmpty();
        assertThat(names.reference("الصبر", null)).isEmpty();
        assertThat(names.reference("120:1", null)).isEmpty();
    }
}
