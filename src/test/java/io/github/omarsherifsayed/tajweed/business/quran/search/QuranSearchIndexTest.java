package io.github.omarsherifsayed.tajweed.business.quran.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Proves the word search on the real search texts (no database): the Java index gives, for every query of
 * {@code src/test/resources/search/reference-queries.csv}, the same number of ayahs and the same first ayah as the
 * independent implementation of the rules that wrote that file — the fixed list of test queries the three clients
 * will also run ({@code AGENTS.md} §8.1, rule 5). Also proves the word positions used for highlighting.
 */
class QuranSearchIndexTest {

    /** The index, built once from the two Tanzil files. */
    private static QuranSearchIndex index;

    /** "surah:ayah" of every ayah, by id − 1. */
    private static List<String> keys;

    /** The Uthmani text of every ayah, by id − 1. */
    private static List<String> uthmani;

    /**
     * Builds the index and reads the keys and the Uthmani text.
     */
    @BeforeAll
    static void build() {
        index = SearchTestFiles.index();
        keys = SearchTestFiles.ayahs("content/tanzil/quran-simple-clean.txt").stream().map(a -> a[0]).toList();
        uthmani = SearchTestFiles.ayahs("content/tanzil/quran-uthmani.txt").stream().map(a -> a[1]).toList();
    }

    @Test
    @DisplayName("Every reference query finds the expected number of ayahs, starting at the expected ayah")
    void matchesTheReferenceCounts() {
        String csv = SearchTestFiles.read("search/reference-queries.csv");
        int checked = 0;
        for (String line : csv.split("\n")) {
            if (line.isBlank() || line.startsWith("#") || line.startsWith("query,")) {
                continue;
            }
            // given
            String[] f = line.strip().split(",");
            // when
            List<Integer> found = index.find(f[0]);
            // then
            assertThat(found).as(f[0]).hasSize(Integer.parseInt(f[1]));
            assertThat(keys.get(found.getFirst() - 1)).as(f[0]).isEqualTo(f[2] + ":" + f[3]);
            checked++;
        }
        assertThat(checked).isEqualTo(10);
    }

    @Test
    @DisplayName("A query without a letter finds nothing")
    void ignoresAQueryWithoutLetters() {
        assertThat(index.find("2:255")).isEmpty();
    }

    @Test
    @DisplayName("112:1 — «قل هو الله أحد» is words 4 to 7 of the Uthmani text, after the four words of the basmala")
    void findsTheWordsToHighlight() {
        int id = keys.indexOf("112:1") + 1;
        assertThat(index.matchedWords(id, "قل هو الله أحد", uthmani.get(id - 1))).containsExactly(4, 5, 6, 7);
        assertThat(index.matchedWords(id, "الله", uthmani.get(id - 1))).containsExactly(1, 6);
    }

    @Test
    @DisplayName("Where the two spellings have a different number of words (2:104, «يَـٰٓأَيُّهَا»), nothing is guessed")
    void doesNotGuessWords() {
        int id = keys.indexOf("2:104") + 1;
        assertThat(index.find("يا أيها الذين آمنوا")).contains(id);
        assertThat(index.matchedWords(id, "يا أيها الذين آمنوا", uthmani.get(id - 1))).isEmpty();
    }
}
