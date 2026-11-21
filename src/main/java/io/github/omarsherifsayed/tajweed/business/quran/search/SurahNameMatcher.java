package io.github.omarsherifsayed.tajweed.business.quran.search;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds a surah by what the user typed, and reads an ayah reference ({@code AGENTS.md} §8.1, the second and third
 * kinds of search).
 *
 * <p>Names come from Tanzil's metadata — the Arabic name, its transliteration and its English name — tidied the same
 * way as the query:
 * <ul>
 *   <li>Arabic: {@link ArabicSearchNormalizer} without marks; a leading «سورة» and the article «ال» are dropped, and
 *       spaces are ignored («البقرة», «سورة البقرة», «بقره» all find 2);</li>
 *   <li>Latin: lower case, letters a – z only, a doubled letter written once, a final «h» after a vowel dropped, a
 *       leading «surah / sura / surat» dropped; the name is also kept without its article («Al-Baqara», «baqarah»,
 *       «al baqara» all find 2); the English name also without «the»;</li>
 *   <li>a number 1 – 114 is the surah of that number.</li>
 * </ul>
 * An exact match comes first; then, for a query of two letters or more, the surahs whose name begins with it.
 * ☐ The list of common spellings of every surah name, from a named source, is still to be added (P1-D3).
 *
 * <p>An ayah reference is {@code 2:255} (also with «.», «/», «-» or a space, and in Arabic-Indic digits), a surah name
 * followed by a number («البقرة 255»), or — with an open surah — a number alone.
 */
public final class SurahNameMatcher {

    /** «surah:ayah» with a separator. */
    private static final Pattern NUMBERS = Pattern.compile("^(\\d{1,3})\\s*[:.\\-/\\s]\\s*(\\d{1,3})$");

    /** A name followed by a number. */
    private static final Pattern NAME_AND_NUMBER = Pattern.compile("^(.*?\\D)\\s*(\\d{1,3})$");

    /** A number alone. */
    private static final Pattern NUMBER = Pattern.compile("^\\d{1,3}$");

    /** The number of surahs. */
    private static final int SURAHS = 114;

    /** The tidied names of each surah, by number − 1. */
    private final List<Set<String>> keys = new ArrayList<>();

    /**
     * Builds the matcher.
     *
     * @param arabic          the Arabic name of every surah, in order (index 0 = surah 1)
     * @param transliteration their names in Latin letters, in order
     * @param english         their English names, in order
     */
    public SurahNameMatcher(List<String> arabic, List<String> transliteration, List<String> english) {
        for (int i = 0; i < arabic.size(); i++) {
            Set<String> set = new LinkedHashSet<>();
            set.add(arabicKey(arabic.get(i)));
            String latin = latinKey(transliteration.get(i));
            set.add(latin);
            String[] parts = transliteration.get(i).split("-", 2);
            if (parts.length == 2 && parts[0].length() <= 3 && parts[0].toLowerCase(Locale.ROOT).startsWith("a")) {
                set.add(latinKey(parts[1]));
            }
            set.add(latinKey(english.get(i)));
            set.add(latinKey(english.get(i).replaceFirst("^(?i)the\\s+", "")));
            set.remove("");
            keys.add(set);
        }
    }

    /**
     * The surahs a query names.
     *
     * @param query what the user typed
     * @return surah numbers: exact matches first, then those whose name begins with the query
     */
    public List<Integer> match(String query) {
        String q = asciiDigits(query).strip();
        List<Integer> result = new ArrayList<>();
        if (NUMBER.matcher(q).matches()) {
            int n = Integer.parseInt(q);
            if (n >= 1 && n <= SURAHS) {
                result.add(n);
            }
            return result;
        }
        Set<String> forms = new LinkedHashSet<>(List.of(arabicKey(q), latinKey(q)));
        forms.remove("");
        for (int i = 0; i < keys.size(); i++) {
            for (String form : forms) {
                if (keys.get(i).contains(form)) {
                    result.add(i + 1);
                    break;
                }
            }
        }
        for (int i = 0; i < keys.size(); i++) {
            if (result.contains(i + 1)) {
                continue;
            }
            for (String form : forms) {
                if (form.length() >= 2 && keys.get(i).stream().anyMatch(k -> k.startsWith(form))) {
                    result.add(i + 1);
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Reads an ayah reference from a query.
     *
     * @param query     what the user typed
     * @param openSurah the surah open in the reader, for a number alone; {@code null} when none
     * @return {surah, ayah} — not yet checked against the surah's number of ayahs; empty when the query is not one
     */
    public Optional<int[]> reference(String query, Integer openSurah) {
        String q = asciiDigits(query).strip();
        Matcher numbers = NUMBERS.matcher(q);
        if (numbers.matches()) {
            return surahAndAyah(Integer.parseInt(numbers.group(1)), numbers.group(2));
        }
        if (openSurah != null && NUMBER.matcher(q).matches()) {
            return surahAndAyah(openSurah, q);
        }
        Matcher named = NAME_AND_NUMBER.matcher(q);
        if (named.matches()) {
            String name = named.group(1).replaceAll("[\\s:.\\-/]+$", "");
            List<Integer> exact = exact(name);
            if (exact.size() == 1) {
                return surahAndAyah(exact.getFirst(), named.group(2));
            }
        }
        return Optional.empty();
    }

    /**
     * The surahs whose tidied name equals the query's, without prefixes.
     *
     * @param name a name
     * @return the surah numbers
     */
    private List<Integer> exact(String name) {
        String arabic = arabicKey(name);
        String latin = latinKey(name);
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i).contains(arabic) || keys.get(i).contains(latin)) {
                result.add(i + 1);
            }
        }
        return result;
    }

    /**
     * A {surah, ayah} pair when the surah exists.
     *
     * @param surah the surah's number
     * @param ayah  the ayah's number, as digits
     * @return the pair, or empty
     */
    private static Optional<int[]> surahAndAyah(int surah, String ayah) {
        int a = Integer.parseInt(ayah);
        if (surah < 1 || surah > SURAHS || a < 1) {
            return Optional.empty();
        }
        return Optional.of(new int[] {surah, a});
    }

    /**
     * The Arabic comparison key of a name.
     *
     * @param name a name or a query
     * @return tidied, without «سورة», the article and spaces; empty when it holds no Arabic letter
     */
    static String arabicKey(String name) {
        String n = ArabicSearchNormalizer.normalize(name, false);
        if (n.codePoints().noneMatch(c -> Character.UnicodeBlock.of(c) == Character.UnicodeBlock.ARABIC
                && Character.isLetter(c))) {
            return "";
        }
        n = n.replaceFirst("^سوره\\s*", "");
        n = n.replaceFirst("^ال", "");
        return n.replace(" ", "");
    }

    /**
     * The Latin comparison key of a name.
     *
     * @param name a name or a query
     * @return lower-case letters a – z, without doubled letters, a final «h» after a vowel, or a leading «surah»
     */
    static String latinKey(String name) {
        String n = name.toLowerCase(Locale.ROOT).replaceFirst("^(surah|surat|sura)\\b", "");
        n = n.replaceAll("[^a-z]", "");
        n = n.replaceAll("(.)\\1+", "$1");
        n = n.replaceFirst("([aeiou])h$", "$1");
        return n;
    }

    /**
     * Arabic-Indic and Persian digits as ASCII digits («٢٥٥» → «255»).
     *
     * @param text any text
     * @return the same text with ASCII digits
     */
    static String asciiDigits(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        text.codePoints().forEach(c -> {
            if (c >= '٠' && c <= '٩') {
                sb.append((char) ('0' + c - '٠'));
            } else if (c >= '۰' && c <= '۹') {
                sb.append((char) ('0' + c - '۰'));
            } else {
                sb.appendCodePoint(c);
            }
        });
        return sb.toString();
    }
}
