package io.github.omarsherifsayed.tajweed.bundle;

import db.migration.V1_1_1__ImportQuranText;
import db.migration.V1_2_1__ImportQuranSearchText;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.flywaydb.core.api.migration.Context;
import org.sqlite.SQLiteConfig;

/**
 * Writes {@code tajweed-quran.sqlite} and its manifest {@code tajweed-quran.manifest.json}.
 *
 * <ol>
 *   <li>creates the tables {@code content_source}, {@code surah}, {@code ayah} and {@code ayah_search_text} with the
 *       server's column names (SQLite types);</li>
 *   <li>runs the server's own import migrations on them — {@code V1_1_1__ImportQuranText} and
 *       {@code V1_2_1__ImportQuranSearchText} — so every checksum and count check of the server runs here too;</li>
 *   <li>checks the result: the counts, <b>every ayah's text and both search texts equal, character for character, to
 *       the Tanzil files</b>, and SQLite's own integrity check;</li>
 *   <li>records the schema version and the content version (the same fingerprint as {@code GET /v1/content-version},
 *       over the bundle's own sources) in {@code bundle_info}, compacts the file, and writes the manifest with the
 *       file's SHA-256.</li>
 * </ol>
 * Any failure stops the build. Nothing in the file is typed or edited by hand ({@code AGENTS.md} §6.1).
 */
public final class QuranBundleBuilder {

    /** The file name of the bundle. */
    static final String FILE = "tajweed-quran.sqlite";

    /** The file name of its manifest. */
    static final String MANIFEST = "tajweed-quran.manifest.json";

    /** The version of the bundle's tables; raised when a table or column changes, so the apps know. */
    static final int SCHEMA_VERSION = 1;

    /** The length of the content-version fingerprint, as the server's. */
    private static final int VERSION_LENGTH = 16;

    /**
     * Not instantiated: run through {@link #main(String[])}.
     */
    private QuranBundleBuilder() {
    }

    /**
     * Builds the bundle.
     *
     * @param args one argument: the folder to write the bundle and its manifest into
     * @throws Exception if a check fails or a file cannot be written — the build stops
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("usage: QuranBundleBuilder <output folder>");
        }
        Path folder = Path.of(args[0]);
        Files.createDirectories(folder);
        Path file = folder.resolve(FILE);
        Files.deleteIfExists(file);
        SQLiteConfig config = new SQLiteConfig();
        config.setDateClass("TEXT");
        config.setDateStringFormat("yyyy-MM-dd");
        config.enforceForeignKeys(true);
        String version;
        try (Connection connection = config.createConnection("jdbc:sqlite:" + file.toAbsolutePath())) {
            connection.setAutoCommit(false);
            createTables(connection);
            Context context = context(connection);
            new V1_1_1__ImportQuranText().migrate(context);
            new V1_2_1__ImportQuranSearchText().migrate(context);
            verify(connection);
            version = contentVersion(connection);
            try (Statement st = connection.createStatement()) {
                // the same file for the same content: the audit columns take the newest source's date, not the clock
                for (String table : new String[] {"content_source", "surah", "ayah", "ayah_search_text"}) {
                    st.execute("update " + table + " set created_at = (select max(obtained_on) from content_source), "
                            + "updated_at = (select max(obtained_on) from content_source)");
                }
                st.execute("create table bundle_info (key text primary key, value text not null)");
                st.execute("insert into bundle_info values ('schema_version', '" + SCHEMA_VERSION + "')");
                st.execute("insert into bundle_info values ('content_version', '" + version + "')");
            }
            connection.commit();
            connection.setAutoCommit(true);
            try (Statement st = connection.createStatement()) {
                st.execute("vacuum");
                try (ResultSet rs = st.executeQuery("pragma integrity_check")) {
                    check(rs.next() && "ok".equals(rs.getString(1)), "SQLite's integrity check");
                }
            }
        }
        byte[] bytes = Files.readAllBytes(file);
        String manifest = "{\n  \"file\": \"" + FILE + "\",\n  \"bytes\": " + bytes.length + ",\n  \"sha256\": \""
                + sha256(bytes) + "\",\n  \"schemaVersion\": " + SCHEMA_VERSION + ",\n  \"contentVersion\": \""
                + version + "\"\n}\n";
        Files.writeString(folder.resolve(MANIFEST), manifest, StandardCharsets.UTF_8);
        System.out.println("Quran bundle written: " + file + " (" + bytes.length + " bytes, content " + version + ")");
    }

    /**
     * Creates the tables with the server's column names, so that the import migrations run unchanged.
     *
     * @param connection the bundle
     * @throws SQLException if a table cannot be created
     */
    static void createTables(Connection connection) throws SQLException {
        String audit = "created_at text not null default current_timestamp, "
                + "updated_at text not null default current_timestamp";
        try (Statement st = connection.createStatement()) {
            st.execute("create table content_source (pk_id integer primary key, code text not null unique, "
                    + "name text not null, resource_path text not null, sha256 text not null, licence text not null, "
                    + "source_url text not null, obtained_on text not null, " + audit + ")");
            st.execute("create table surah (pk_id integer primary key check (pk_id between 1 and 114), "
                    + "name_ar text not null, name_transliteration text not null, name_en text not null, "
                    + "revelation_type text not null check (revelation_type in ('MECCAN', 'MEDINAN')), "
                    + "revelation_order integer not null unique, ayah_count integer not null, " + audit + ")");
            st.execute("create table ayah (pk_id integer primary key check (pk_id between 1 and 6236), "
                    + "surah_id integer not null references surah (pk_id), number_in_surah integer not null, "
                    + "text_uthmani text not null, basmala_word_count integer not null check "
                    + "(basmala_word_count in (0, 4)), page integer not null, juz integer not null, "
                    + "hizb_quarter integer not null, sajdah_type text check (sajdah_type in "
                    + "('RECOMMENDED', 'OBLIGATORY')), " + audit + ", unique (surah_id, number_in_surah))");
            st.execute("create index ix_ayah_page on ayah (page)");
            st.execute("create index ix_ayah_juz on ayah (juz)");
            st.execute("create table ayah_search_text (ayah_id integer primary key references ayah (pk_id), "
                    + "text_simple text not null, text_simple_clean text not null, " + audit + ")");
        }
    }

    /**
     * Checks the bundle against the source files themselves, not against the import's own results.
     *
     * @param connection the bundle
     * @throws SQLException if it cannot be read
     * @throws IOException  if a source file cannot be read
     */
    static void verify(Connection connection) throws SQLException, IOException {
        check(count(connection, "select count(*) from surah") == 114, "114 surahs");
        check(count(connection, "select count(*) from ayah") == 6236, "6,236 ayahs");
        check(count(connection, "select count(*) from ayah_search_text") == 6236, "6,236 search-text rows");
        check(count(connection, "select count(distinct page) from ayah") == 604, "604 pages");
        check(count(connection, "select count(*) from content_source") == 5, "the five sources of the Quran text");
        compare(connection, "select surah_id || '|' || number_in_surah || '|' || text_uthmani from ayah order by pk_id",
                lines("content/tanzil/quran-uthmani.txt"), "the Uthmani text");
        compare(connection, "select a.surah_id || '|' || a.number_in_surah || '|' || s.text_simple from ayah_search_text s "
                + "join ayah a on a.pk_id = s.ayah_id order by s.ayah_id",
                lines("content/tanzil/quran-simple.txt"), "the Simple text");
        compare(connection, "select a.surah_id || '|' || a.number_in_surah || '|' || s.text_simple_clean "
                + "from ayah_search_text s join ayah a on a.pk_id = s.ayah_id order by s.ayah_id",
                lines("content/tanzil/quran-simple-clean.txt"), "the Simple (Clean) text");
    }

    /**
     * The content version over the bundle's sources — computed as the server computes it.
     *
     * @param connection the bundle
     * @return 16 hexadecimal digits
     * @throws SQLException if it cannot be read
     */
    static String contentVersion(Connection connection) throws SQLException {
        StringBuilder all = new StringBuilder();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("select code, sha256 from content_source order by pk_id")) {
            while (rs.next()) {
                all.append(rs.getString(1)).append('=').append(rs.getString(2)).append('\n');
            }
        }
        return sha256(all.toString().getBytes(StandardCharsets.UTF_8)).substring(0, VERSION_LENGTH);
    }

    /**
     * Flyway's migration context, giving the bundle's connection to the import migrations (they use nothing else).
     *
     * @param connection the bundle
     * @return the context
     */
    private static Context context(Connection connection) {
        return (Context) Proxy.newProxyInstance(Context.class.getClassLoader(), new Class<?>[] {Context.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getConnection")) {
                        return connection;
                    }
                    throw new UnsupportedOperationException("not needed by the import: " + method.getName());
                });
    }

    /**
     * Compares a query's rows with a file's lines, in order.
     *
     * @param connection the bundle
     * @param sql        a query returning one text column
     * @param expected   the lines it must equal
     * @param what       what is compared
     * @throws SQLException if it cannot be read
     */
    private static void compare(Connection connection, String sql, List<String> expected, String what)
            throws SQLException {
        List<String> stored = new ArrayList<>();
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                stored.add(rs.getString(1));
            }
        }
        check(stored.equals(expected), what + " equal to its file, line by line");
    }

    /**
     * The {@code surah|ayah|text} lines of a Tanzil file on the class path, comments skipped.
     *
     * @param path the file
     * @return its lines, in order
     * @throws IOException if it cannot be read
     */
    private static List<String> lines(String path) throws IOException {
        List<String> out = new ArrayList<>();
        try (var in = QuranBundleBuilder.class.getClassLoader().getResourceAsStream(path)) {
            check(in != null, "the file " + path);
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                if (!line.isEmpty() && !line.startsWith("#")) {
                    out.add(line);
                }
            }
        }
        return out;
    }

    /**
     * Runs a counting query.
     *
     * @param connection the bundle
     * @param sql        a query returning one number
     * @return the number
     * @throws SQLException if it cannot be read
     */
    private static int count(Connection connection, String sql) throws SQLException {
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /**
     * The SHA-256 of some bytes.
     *
     * @param bytes the bytes
     * @return 64 lower-case hexadecimal digits
     */
    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /**
     * Stops the build when a condition does not hold.
     *
     * @param condition what must be true
     * @param what      what was checked
     * @throws IllegalStateException if the condition is false
     */
    private static void check(boolean condition, String what) {
        if (!condition) {
            throw new IllegalStateException("Quran bundle stopped — check failed: " + what);
        }
    }
}
