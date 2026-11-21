package io.github.omarsherifsayed.tajweed.business.quran.search;

import java.text.Normalizer;

/**
 * The one function that tidies Arabic for searching — applied the same way to what the user types and to the search
 * text when the index is built ({@code AGENTS.md} §8.1, rule 3; the letter equivalences fixed in task P1-D3).
 *
 * <p>It works only on the <b>search text</b> (Tanzil «Simple» / «Simple (Clean)»), which is never displayed; the Quran
 * text the reader shows is never passed through it (§6.1). The rules, in order:
 * <ol>
 *   <li>Unicode NFC, so that marks typed in any order compare equal (shadda and fatha, for example);</li>
 *   <li>the tatweel (U+0640) is removed;</li>
 *   <li>the Quranic annotation signs U+06D6 – U+06ED (pause marks, small high letters, ۞ and ۩) are removed — they
 *       are not letters;</li>
 *   <li>the forms of alef أ إ آ ٱ become ا;</li>
 *   <li>ى becomes ي, and ة becomes ه (both are commonly typed either way);</li>
 *   <li>without tashkeel ({@code keepMarks = false}), every vowel mark is removed (the fathah, the shadda, the small
 *       alef …); with tashkeel they are kept, so the marks the user typed must agree;</li>
 *   <li>runs of spaces become one space; the ends are trimmed.</li>
 * </ol>
 * The same rules are implemented independently in a test (from the reference counts of
 * {@code src/test/resources/search/reference-queries.csv}), so a change here that alters results is noticed.
 */
public final class ArabicSearchNormalizer {

    /** The tatweel (kashida), a stretching sign — not a letter. */
    private static final char TATWEEL = 'ـ';

    /** The first Quranic annotation sign (small high ligature sad with lam with alef maksura). */
    private static final char ANNOTATION_FIRST = 'ۖ';

    /** The last Quranic annotation sign (small low meem). */
    private static final char ANNOTATION_LAST = 'ۭ';

    /**
     * Not instantiated: static methods only.
     */
    private ArabicSearchNormalizer() {
    }

    /**
     * Whether a text carries tashkeel (any vowel or other combining mark) — decides which search text is used.
     *
     * @param text what the user typed
     * @return {@code true} when it holds at least one combining mark
     */
    public static boolean hasMarks(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFC).codePoints()
                .anyMatch(c -> Character.getType(c) == Character.NON_SPACING_MARK && !isAnnotation(c));
    }

    /**
     * Tidies a text for searching, by the rules above.
     *
     * @param text      the user's query, or a line of the search text
     * @param keepMarks {@code true} to keep the vowel marks (a query typed with tashkeel)
     * @return the tidied text
     */
    public static String normalize(String text, boolean keepMarks) {
        String nfc = Normalizer.normalize(text, Normalizer.Form.NFC);
        StringBuilder out = new StringBuilder(nfc.length());
        boolean space = false;
        for (int i = 0; i < nfc.length(); i++) {
            char c = nfc.charAt(i);
            if (c == TATWEEL || isAnnotation(c)) {
                continue;
            }
            if (!keepMarks && Character.getType(c) == Character.NON_SPACING_MARK) {
                continue;
            }
            if (Character.isWhitespace(c)) {
                space = !out.isEmpty();
                continue;
            }
            if (space) {
                out.append(' ');
                space = false;
            }
            out.append(fold(c));
        }
        return out.toString();
    }

    /**
     * The letter a letter is searched as (rules 4 and 5).
     *
     * @param c a character
     * @return the same character, or its plain form
     */
    private static char fold(char c) {
        return switch (c) {
            case 'أ', 'إ', 'آ', 'ٱ' -> 'ا';
            case 'ى' -> 'ي';
            case 'ة' -> 'ه';
            default -> c;
        };
    }

    /**
     * Whether a character is a Quranic annotation sign (U+06D6 – U+06ED).
     *
     * @param c a code point
     * @return {@code true} for the pause marks, small high letters, ۞ and ۩
     */
    private static boolean isAnnotation(int c) {
        return c >= ANNOTATION_FIRST && c <= ANNOTATION_LAST;
    }
}
