package io.github.omarsherifsayed.tajweed.business.quran.search;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the content files the search tests need, straight from the class path (no database): the Tanzil texts and the
 * surah names of Tanzil's metadata.
 */
final class SearchTestFiles {

    /**
     * Not instantiated.
     */
    private SearchTestFiles() {
    }

    /**
     * The {@code surah|ayah|text} lines of a Tanzil text, in order, comments skipped.
     *
     * @param path the file on the class path
     * @return pairs {"surah:ayah", text}
     */
    static List<String[]> ayahs(String path) {
        List<String[]> out = new ArrayList<>();
        for (String line : read(path).split("\n")) {
            if (!line.isEmpty() && !line.startsWith("#")) {
                String[] p = line.split("\\|", 3);
                out.add(new String[] {p[0] + ":" + p[1], p[2]});
            }
        }
        return out;
    }

    /**
     * The index built from the two search texts.
     *
     * @return the index
     */
    static QuranSearchIndex index() {
        return new QuranSearchIndex(texts("content/tanzil/quran-simple.txt"),
                texts("content/tanzil/quran-simple-clean.txt"));
    }

    /**
     * The surah-name matcher built from Tanzil's metadata.
     *
     * @return the matcher
     */
    static SurahNameMatcher names() {
        String xml = read("content/tanzil/quran-data.xml");
        Matcher m = Pattern.compile("<sura [^>]*name=\"([^\"]*)\"[^>]*tname=\"([^\"]*)\"[^>]*ename=\"([^\"]*)\"")
                .matcher(xml);
        List<String> ar = new ArrayList<>();
        List<String> tr = new ArrayList<>();
        List<String> en = new ArrayList<>();
        while (m.find()) {
            ar.add(m.group(1));
            tr.add(m.group(2));
            en.add(m.group(3));
        }
        return new SurahNameMatcher(ar, tr, en);
    }

    /**
     * The texts only of a Tanzil file.
     *
     * @param path the file
     * @return the ayahs' texts, in order
     */
    private static List<String> texts(String path) {
        return ayahs(path).stream().map(a -> a[1]).toList();
    }

    /**
     * A class-path file as UTF-8.
     *
     * @param path the file
     * @return its content
     */
    static String read(String path) {
        try (InputStream in = SearchTestFiles.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("missing " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
