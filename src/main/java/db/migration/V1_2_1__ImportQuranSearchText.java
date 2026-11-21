package db.migration;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Imports the search text ({@code ayah_search_text}): Tanzil «Simple» and «Simple (Clean)», byte for byte, beside each
 * ayah ({@code AGENTS.md} §8.1, rules 1 – 2; P1-D3). Never displayed — it only lets a user find an ayah by typing the
 * everyday spelling, with or without tashkeel.
 *
 * <p>As with the Quran text: both files' SHA-256 are verified first; then both must hold the 6,236 ayahs in exactly the
 * order of the {@code ayah} table; only then are the rows written. The files were downloaded on 2026-11-21 with the
 * author's permission («Yes, download both»); their origin is in {@code docs/data-sources/README.md}.
 */
public class V1_2_1__ImportQuranSearchText extends BaseJavaMigration {

    /** Tanzil Quran Text (Simple, Version 1.1): everyday spelling with tashkeel. */
    static final String SIMPLE_FILE = "content/tanzil/quran-simple.txt";

    /** Its SHA-256 (downloaded 2026-11-21). */
    static final String SIMPLE_SHA256 = "b3638f5cf03e2b05e4000efc0e4b656aafb3cf6ed28f8770b6497c5b43c46089";

    /** Tanzil Quran Text (Simple Clean, Version 1.1): everyday spelling without tashkeel. */
    static final String CLEAN_FILE = "content/tanzil/quran-simple-clean.txt";

    /** Its SHA-256 (downloaded 2026-11-21). */
    static final String CLEAN_SHA256 = "1862ee63c70fbfc6285e0bb0b9587b89beb5b7891109a4d543d9d2cf8283e8c8";

    /** The address both were downloaded from (the text type and the options in the query). */
    private static final String URL = "https://tanzil.net/download/";

    /**
     * Created by Flyway.
     */
    public V1_2_1__ImportQuranSearchText() {
        // no state
    }

    /**
     * Verifies, checks the order against the {@code ayah} table, and imports.
     *
     * @param context Flyway's context, giving the connection of this migration's transaction
     * @throws Exception if a file is changed or the ayahs do not line up — the migration is rolled back
     */
    @Override
    public void migrate(Context context) throws Exception {
        List<String[]> simple = lines(V1_1_1__ImportQuranText.read(SIMPLE_FILE, SIMPLE_SHA256));
        List<String[]> clean = lines(V1_1_1__ImportQuranText.read(CLEAN_FILE, CLEAN_SHA256));
        Connection connection = context.getConnection();
        List<String> keys = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("select surah_id, number_in_surah from ayah order by pk_id")) {
            while (rs.next()) {
                keys.add(rs.getInt(1) + "|" + rs.getInt(2));
            }
        }
        check(keys.size() == 6236 && simple.size() == keys.size() && clean.size() == keys.size(),
                "6,236 ayahs in each file");
        String sql = "insert into ayah_search_text (ayah_id, text_simple, text_simple_clean) values (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < keys.size(); i++) {
                check(simple.get(i)[0].equals(keys.get(i)) && clean.get(i)[0].equals(keys.get(i)),
                        "the ayah order at " + keys.get(i));
                ps.setInt(1, i + 1);
                ps.setString(2, simple.get(i)[1]);
                ps.setString(3, clean.get(i)[1]);
                ps.addBatch();
                if ((i + 1) % 500 == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
        }
        String src = "insert into content_source (code, name, resource_path, sha256, licence, source_url, obtained_on) "
                + "values (?, ?, ?, ?, 'CC BY 3.0 — verbatim copies only', ?, ?)";
        List<String[]> sources = List.of(
                new String[] {"TANZIL_SIMPLE", "Tanzil Quran Text (Simple, Version 1.1)", SIMPLE_FILE, SIMPLE_SHA256},
                new String[] {"TANZIL_SIMPLE_CLEAN", "Tanzil Quran Text (Simple Clean, Version 1.1)", CLEAN_FILE,
                    CLEAN_SHA256});
        try (PreparedStatement ps = connection.prepareStatement(src)) {
            for (String[] s : sources) {
                ps.setString(1, s[0]);
                ps.setString(2, s[1]);
                ps.setString(3, s[2]);
                ps.setString(4, s[3]);
                ps.setString(5, URL);
                ps.setDate(6, Date.valueOf(LocalDate.of(2026, 11, 21)));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * The {@code surah|ayah|text} lines of a file, in order, as pairs {"surah|ayah", text}; comments and empty lines
     * are skipped, and the text is kept exactly as written.
     *
     * @param bytes the file's bytes
     * @return the lines
     */
    static List<String[]> lines(byte[] bytes) {
        List<String[]> out = new ArrayList<>();
        for (String line : new String(bytes, StandardCharsets.UTF_8).split("\n", -1)) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] p = line.split("\\|", 3);
            check(p.length == 3, "a line without surah|ayah|text");
            out.add(new String[] {p[0] + "|" + p[1], p[2]});
        }
        return out;
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
            throw new IllegalStateException("Search text import stopped — check failed: " + what);
        }
    }
}
