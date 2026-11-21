package io.github.omarsherifsayed.tajweed.business.quran.search;

import java.util.ArrayList;
import java.util.List;

/**
 * The search index of the Quran: the search text of every ayah, tidied once by {@link ArabicSearchNormalizer}, kept in
 * memory (about 2 MB) and scanned for each query — 6,236 short strings, a few milliseconds.
 *
 * <p>Two tidied copies are kept: «Simple (Clean)» without marks (for a query typed without tashkeel) and «Simple» with
 * its marks (for a query typed with tashkeel, rule 4 of {@code AGENTS.md} §8.1). A match is the tidied query found
 * anywhere inside the tidied ayah — a word, a part of a word, or several words in a row. The index never holds or
 * changes the displayed Uthmani text.
 */
public final class QuranSearchIndex {

    /** Tidied «Simple (Clean)», by ayah id − 1. */
    private final String[] clean;

    /** Tidied «Simple», marks kept, by ayah id − 1. */
    private final String[] marked;

    /**
     * Builds the index.
     *
     * @param simple      «Simple» text of every ayah, in the order of the mushaf (index 0 = ayah id 1)
     * @param simpleClean «Simple (Clean)» text of every ayah, in the same order
     */
    public QuranSearchIndex(List<String> simple, List<String> simpleClean) {
        if (simple.size() != simpleClean.size()) {
            throw new IllegalArgumentException("the two search texts have different numbers of ayahs");
        }
        clean = new String[simpleClean.size()];
        marked = new String[simple.size()];
        for (int i = 0; i < clean.length; i++) {
            clean[i] = ArabicSearchNormalizer.normalize(simpleClean.get(i), false);
            marked[i] = ArabicSearchNormalizer.normalize(simple.get(i), true);
        }
    }

    /**
     * The ayahs that contain a query.
     *
     * @param query what the user typed (not yet tidied)
     * @return the ids of the matching ayahs, in the order of the mushaf; empty when the query holds no letter
     */
    public List<Integer> find(String query) {
        boolean marks = ArabicSearchNormalizer.hasMarks(query);
        String q = ArabicSearchNormalizer.normalize(query, marks);
        List<Integer> ids = new ArrayList<>();
        if (!hasLetter(q)) {
            return ids;
        }
        String[] texts = marks ? marked : clean;
        for (int i = 0; i < texts.length; i++) {
            if (texts[i].contains(q)) {
                ids.add(i + 1);
            }
        }
        return ids;
    }

    /**
     * Which words of the displayed Uthmani text hold the match, so that a client can highlight them (a display layer,
     * {@code AGENTS.md} §6.3).
     *
     * <p>A word is a space-separated part of the Uthmani text; the result counts them from 0 over the whole text
     * (the basmala's words included). The search text and the Uthmani text are matched word by word only when they
     * have the same number of words that hold a letter; they do not in 363 ayahs (the Uthmani spelling joins or splits
     * some words, such as «يَـٰٓأَيُّهَا» against «يا أيها»), and there the result is empty — the client highlights
     * the whole ayah.
     *
     * @param ayahId  the ayah, 1 to 6,236
     * @param query   what the user typed (not yet tidied)
     * @param uthmani the ayah's Uthmani text, as stored
     * @return the indexes of the matched words, ascending; empty when they cannot be told exactly
     */
    public List<Integer> matchedWords(int ayahId, String query, String uthmani) {
        boolean marks = ArabicSearchNormalizer.hasMarks(query);
        String q = ArabicSearchNormalizer.normalize(query, marks);
        String text = (marks ? marked : clean)[ayahId - 1];
        List<int[]> searchWords = letterWords(text);
        String[] tokens = uthmani.split(" ", -1);
        List<Integer> uthmaniWords = new ArrayList<>();
        for (int i = 0; i < tokens.length; i++) {
            if (hasLetter(tokens[i])) {
                uthmaniWords.add(i);
            }
        }
        List<Integer> result = new ArrayList<>();
        if (q.isEmpty() || searchWords.size() != uthmaniWords.size()) {
            return result;
        }
        for (int at = text.indexOf(q); at >= 0; at = text.indexOf(q, at + q.length())) {
            int end = at + q.length();
            for (int w = 0; w < searchWords.size(); w++) {
                int[] span = searchWords.get(w);
                Integer index = uthmaniWords.get(w);
                if (span[0] < end && span[1] > at && !result.contains(index)) {
                    result.add(index);
                }
            }
        }
        result.sort(null);
        return result;
    }

    /**
     * The words of a tidied text that hold a letter, as {start, end} character offsets.
     *
     * @param text a tidied text (single spaces)
     * @return the spans, in order
     */
    private static List<int[]> letterWords(String text) {
        List<int[]> spans = new ArrayList<>();
        int start = 0;
        for (int i = 0; i <= text.length(); i++) {
            if (i == text.length() || text.charAt(i) == ' ') {
                if (i > start && hasLetter(text.substring(start, i))) {
                    spans.add(new int[] {start, i});
                }
                start = i + 1;
            }
        }
        return spans;
    }

    /**
     * Whether a text holds at least one letter.
     *
     * @param text any text
     * @return {@code true} when it does
     */
    private static boolean hasLetter(String text) {
        return text.codePoints().anyMatch(Character::isLetter);
    }
}
