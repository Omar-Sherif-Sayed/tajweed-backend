package db.migration;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Imports the lines of the printed Madinah mushaf, 1405H ({@code mushaf_line}; F2, {@code AGENTS.md} §8.1; P0-12;
 * the author approved the table on 2026-11-26): where each of the 604 pages breaks its lines, as positions in the
 * Tanzil text — never as text of its own (§6.1).
 *
 * <p>The line breaks come from the DigitalKhatt old-Madinah layout ({@code quran_text_old_madinah.ts}, MIT), read as
 * text and never executed. Its words are written in another Unicode encoding than Tanzil's, so each of its words is
 * matched to the next Tanzil word by a <em>skeleton</em> — the letters only, marks and format characters dropped, the
 * alef and hamza seats unified — a comparison key that is never shown. The file's SHA-256 is verified first, and the
 * import stops unless: every one of the 77,433 Tanzil words with letters is matched in order; every Tanzil token (the
 * pause marks and the sajdah sign included, the basmala in front of ayah 1 excepted) lies on exactly one line, in
 * order; there are 604 pages of 15 lines (pages 1 and 2: 8), 114 surah headers and 112 basmala lines; every ayah
 * starts on the page Tanzil's metadata gives it; and every page begins with a new ayah (the clients rely on it). Checked against the author's printed copy on 2026-11-17 (pages 1, 2,
 * 5, 263, 264, 545, 546 — {@code tajweed-docs/docs/design/prototypes/assets/README.md}).
 */
public class V1_7_1__ImportMushafLines extends BaseJavaMigration {

    /** The DigitalKhatt old-Madinah layout. */
    static final String LAYOUT_FILE = "content/digitalkhatt/quran_text_old_madinah.ts";

    /** Its SHA-256 (downloaded 2026-11-17, commit 4213fab9892774f74f7164c70a70dd84a14f0215). */
    static final String LAYOUT_SHA256 = "08972d48c6c250fda4ce9c58a2f23be52c17b0cf097ac093a040e3c392b79f07";

    /** Where it was obtained. */
    private static final String LAYOUT_URL =
            "https://github.com/DigitalKhatt/digitalkhatt.org/blob/4213fab9892774f74f7164c70a70dd84a14f0215/"
                    + "ClientApp/src/app/services/quran_text_old_madinah.ts";

    /** The end-of-ayah sign. */
    private static final char AYAH_END = '۝';

    /** The rub el hizb sign. */
    private static final char RUB = '۞';

    /** The sajdah sign. */
    private static final char SAJDAH = '۩';

    /** The combining grapheme joiner the layout puts between some letters and marks. */
    private static final String CGJ = "͏";

    /** Splits the file into its 604 page blocks. */
    private static final Pattern PAGE_END = Pattern.compile("\n  \\],?\n?");

    /** One quoted line of a page block. */
    private static final Pattern LINE = Pattern.compile("^\\s{4}'([^'\n]*)',?$", Pattern.MULTILINE);

    /** The header line of a surah. */
    private static final String HEADER_PREFIX = "سُورَةُ";

    /**
     * One line found in the layout.
     *
     * @param page  the page, from 1
     * @param line  the line, from 1
     * @param kind  {@code SURAH_HEADER}, {@code BASMALA} or {@code TEXT}
     * @param surah the surah of a header or a basmala, otherwise {@code null}
     * @param first a text line's first word: {ayah index from 0, word index from 0}
     * @param last  a text line's last word, likewise
     */
    record Line(int page, int line, String kind, Integer surah, int[] first, int[] last) {
    }

    /**
     * Created by Flyway.
     */
    public V1_7_1__ImportMushafLines() {
        // no state
    }

    /**
     * Verifies the file, maps every line onto the Tanzil words, checks the result, and imports.
     *
     * @param context Flyway's context, giving the connection of this migration's transaction
     * @throws Exception if the file is changed or a check fails — the migration is rolled back
     */
    @Override
    public void migrate(Context context) throws Exception {
        List<List<String>> pages = pages(V1_1_1__ImportQuranText.read(LAYOUT_FILE, LAYOUT_SHA256));
        Connection connection = context.getConnection();
        List<String[]> tokens = new ArrayList<>();       // index = ayah id - 1: the text's space-separated words
        List<Integer> skip = new ArrayList<>();           // the basmala's words in front of ayah 1, otherwise 0
        List<Integer> pageOf = new ArrayList<>();         // the page Tanzil's metadata gives the ayah
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("select text_uthmani, basmala_word_count, page from ayah order by pk_id")) {
            while (rs.next()) {
                tokens.add(rs.getString(1).split(" ", -1));
                skip.add(rs.getInt(2));
                pageOf.add(rs.getInt(3));
            }
        }
        check(tokens.size() == 6236, "6,236 ayahs in the ayah table");
        List<Line> lines = map(pages, tokens, skip);
        verify(pages, lines, tokens, skip, pageOf);
        insert(connection, lines);
    }

    /**
     * The page blocks of the layout, each a list of its lines (the joiner U+034F removed — it only matters to
     * drawing, and nothing here is drawn).
     *
     * @param bytes the file's bytes
     * @return 604 pages
     */
    static List<List<String>> pages(byte[] bytes) {
        String text = new String(bytes, StandardCharsets.UTF_8).replace("\r\n", "\n");
        List<List<String>> out = new ArrayList<>();
        for (String block : PAGE_END.split(text)) {
            List<String> lines = new ArrayList<>();
            Matcher m = LINE.matcher(block);
            while (m.find()) {
                lines.add(m.group(1).replace(CGJ, ""));
            }
            if (!lines.isEmpty()) {
                out.add(lines);
            }
        }
        return out;
    }

    /**
     * Maps the layout's lines onto Tanzil positions {ayah index from 0, word index from 0}.
     *
     * @param pages  the layout's pages
     * @param tokens the Tanzil words of each ayah
     * @param skip   the basmala's words in front of each ayah
     * @return every line, in order
     */
    static List<Line> map(List<List<String>> pages, List<String[]> tokens, List<Integer> skip) {
        // The Tanzil words that have letters, in order — the stream the layout's words are matched to.
        List<int[]> stream = new ArrayList<>();
        for (int a = 0; a < tokens.size(); a++) {
            for (int w = skip.get(a); w < tokens.get(a).length; w++) {
                if (!skeleton(tokens.get(a)[w]).isEmpty()) {
                    stream.add(new int[] {a, w});
                }
            }
        }
        String basmala = skeleton(String.join("", tokens.get(0)));
        List<Line> out = new ArrayList<>();
        int pos = 0;
        int surah = 0;
        for (int p = 0; p < pages.size(); p++) {
            for (int l = 0; l < pages.get(p).size(); l++) {
                String line = pages.get(p).get(l);
                if (line.startsWith(HEADER_PREFIX) && line.indexOf(AYAH_END) < 0) {
                    surah++;
                    out.add(new Line(p + 1, l + 1, "SURAH_HEADER", surah, null, null));
                    continue;
                }
                if (surah > 1 && line.indexOf(AYAH_END) < 0 && skeleton(line.replace(" ", "")).equals(basmala)) {
                    out.add(new Line(p + 1, l + 1, "BASMALA", surah, null, null));
                    continue;
                }
                int[] first = null;
                int[] last = null;
                for (String token : line.split(" ")) {
                    String s = skeleton(token);
                    if (s.isEmpty()) {
                        continue;                       // an ayah number, a quarter or a sajdah sign
                    }
                    check(pos < stream.size(), "the layout has more words than Tanzil (page " + (p + 1) + ")");
                    int[] at = stream.get(pos++);
                    check(skeleton(tokens.get(at[0])[at[1]]).equals(s),
                            "page " + (p + 1) + " line " + (l + 1) + ": the layout's word does not match Tanzil's");
                    if (first == null) {
                        first = at;
                    }
                    last = at;
                }
                check(first != null, "page " + (p + 1) + " line " + (l + 1) + " has no words");
                // The marks written as their own tokens after the line's last word (pause marks, the sajdah sign)
                // belong to that line.
                int w = last[1];
                while (w + 1 < tokens.get(last[0]).length && skeleton(tokens.get(last[0])[w + 1]).isEmpty()) {
                    w++;
                }
                out.add(new Line(p + 1, l + 1, "TEXT", null, first, new int[] {last[0], w}));
            }
        }
        check(pos == stream.size(), "every Tanzil word matched (" + pos + " of " + stream.size() + ")");
        return out;
    }

    /**
     * The checks the import must pass before anything is written.
     *
     * @param pages  the layout's pages
     * @param lines  the mapped lines
     * @param tokens the Tanzil words of each ayah
     * @param skip   the basmala's words in front of each ayah
     * @param pageOf the page Tanzil's metadata gives each ayah
     */
    static void verify(List<List<String>> pages, List<Line> lines, List<String[]> tokens, List<Integer> skip,
            List<Integer> pageOf) {
        check(pages.size() == 604, "604 pages");
        for (int p = 0; p < pages.size(); p++) {
            check(pages.get(p).size() == (p < 2 ? 8 : 15), "the lines of page " + (p + 1));
        }
        check(lines.stream().filter(x -> x.kind().equals("SURAH_HEADER")).count() == 114, "114 surah headers");
        check(lines.stream().filter(x -> x.kind().equals("BASMALA")).count() == 112, "112 basmala lines");
        // Every token lies on exactly one line, in order: walk the lines and the text together.
        int a = 0;
        int w = skip.get(0);
        for (Line x : lines) {
            if (!x.kind().equals("TEXT")) {
                continue;
            }
            check(x.first()[0] == a && x.first()[1] == w,
                    "page " + x.page() + " line " + x.line() + " starts where the previous line ended");
            if (w == skip.get(a)) {
                check(pageOf.get(a) == x.page(), "ayah " + (a + 1) + " starts on its page");
            } else {
                check(x.line() > 1 && lines.stream().anyMatch(y -> y.page() == x.page() && y.line() < x.line()
                        && y.kind().equals("TEXT")), "page " + x.page() + " begins with a new ayah");
            }
            a = x.last()[0];
            w = x.last()[1] + 1;
            if (w == tokens.get(a).length) {
                a++;
                w = a < tokens.size() ? skip.get(a) : 0;
            }
        }
        check(a == tokens.size(), "every word of the Quran lies on a line");
    }

    /**
     * Writes the lines and records the layout's source.
     *
     * @param connection the migration's connection
     * @param lines      the lines
     * @throws Exception if the database refuses a row
     */
    private static void insert(Connection connection, List<Line> lines) throws Exception {
        String sql = "insert into mushaf_line (page, line, kind, surah_number, first_ayah_id, first_word, "
                + "last_ayah_id, last_word) values (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int n = 0;
            for (Line x : lines) {
                ps.setInt(1, x.page());
                ps.setInt(2, x.line());
                ps.setString(3, x.kind());
                if (x.surah() == null) {
                    ps.setNull(4, Types.SMALLINT);
                    ps.setInt(5, x.first()[0] + 1);
                    ps.setInt(6, x.first()[1]);
                    ps.setInt(7, x.last()[0] + 1);
                    ps.setInt(8, x.last()[1]);
                } else {
                    ps.setInt(4, x.surah());
                    for (int i = 5; i <= 8; i++) {
                        ps.setNull(i, Types.INTEGER);
                    }
                }
                ps.addBatch();
                if (++n % 1000 == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
        }
        String src = "insert into content_source (code, name, resource_path, sha256, licence, source_url, obtained_on) "
                + "values (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(src)) {
            ps.setString(1, "DIGITALKHATT_OLD_MADINAH_LAYOUT");
            ps.setString(2, "DigitalKhatt — old Madinah mushaf layout (1405H), line breaks only");
            ps.setString(3, LAYOUT_FILE);
            ps.setString(4, LAYOUT_SHA256);
            ps.setString(5, "MIT");
            ps.setString(6, LAYOUT_URL);
            ps.setDate(7, Date.valueOf(LocalDate.of(2026, 11, 17)));
            ps.executeUpdate();
        }
    }

    /**
     * A word's letters only — the comparison key between the two encodings, never shown: decomposed; marks, format
     * characters, modifier letters, digits and the ayah, quarter and sajdah signs dropped; alef wasla read as alef,
     * the free-standing hamza dropped, the Farsi yeh and alef maksura read as yeh.
     *
     * @param word a word
     * @return its skeleton (empty for a sign or a pause mark)
     */
    static String skeleton(String word) {
        StringBuilder out = new StringBuilder();
        String d = Normalizer.normalize(word, Normalizer.Form.NFD);
        for (int i = 0; i < d.length(); i++) {
            char ch = d.charAt(i);
            int type = Character.getType(ch);
            if (type == Character.NON_SPACING_MARK || type == Character.ENCLOSING_MARK
                    || type == Character.COMBINING_SPACING_MARK || type == Character.FORMAT
                    || type == Character.MODIFIER_LETTER || Character.isDigit(ch)
                    || ch == RUB || ch == SAJDAH || ch == AYAH_END) {
                continue;
            }
            out.append(ch);
        }
        return out.toString().replace('ٱ', 'ا').replace("ء", "").replace('ی', 'ي').replace('ى', 'ي');
    }

    /**
     * Stops the import when a condition does not hold.
     *
     * @param condition what must be true
     * @param what      what was being checked
     * @throws IllegalStateException if the condition is false
     */
    private static void check(boolean condition, String what) {
        if (!condition) {
            throw new IllegalStateException("Mushaf line import stopped — check failed: " + what);
        }
    }
}
