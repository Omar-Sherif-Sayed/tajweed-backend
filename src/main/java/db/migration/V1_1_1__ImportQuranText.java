package db.migration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Imports the Quran text and its metadata into {@code surah} and {@code ayah} ({@code AGENTS.md} §6, P1-D1, P1-D2).
 *
 * <p>The order of the work is the safety of the data:
 * <ol>
 *   <li>every source file is read from the application ({@code src/main/resources/content/}) and its SHA-256 is
 *       compared with the value recorded below — one changed byte stops the migration, and the application does not
 *       start;</li>
 *   <li>the files are parsed and every count is checked: 114 surahs, 6,236 ayahs, each surah's ayahs numbered 1 to its
 *       count, 604 pages, 30 juz, 240 quarters, 15 sajdah places, the basmala at the head of 112 first ayahs;</li>
 *   <li>only then are the rows written — the text of every ayah byte for byte as Tanzil gives it, never edited,
 *       normalised or cleaned (§6.1).</li>
 * </ol>
 *
 * <p>Sources: Tanzil Quran Text (Uthmani, 1.1), Tanzil Quran Metadata 1.0 (both CC BY 3.0, verbatim; their notices
 * stay in the files) and our reviewed table of quarter corrections. Their origins and checks are written in
 * {@code docs/data-sources/README.md} and {@code docs/database/README.md}.
 */
public class V1_1_1__ImportQuranText extends BaseJavaMigration {

    /** The Tanzil Uthmani text, one {@code surah|ayah|text} line per ayah. */
    static final String TEXT_FILE = "content/tanzil/quran-uthmani.txt";

    /** Its SHA-256, recorded when it was downloaded (2026-11-17) and checked in the design prototypes since. */
    static final String TEXT_SHA256 = "6933e133dd56db778c801bf738848454e43648105a151e8d84d86a7cae39ec5f";

    /** The Tanzil metadata: surahs, juz, hizb quarters, pages and sajdah places of the Madinah mushaf. */
    static final String META_FILE = "content/tanzil/quran-data.xml";

    /** Its SHA-256 (2026-11-17). */
    static final String META_SHA256 = "8867c1d88191472adec9db694b3cd9f135b1a2ef580574d32cf888dcb22c5c7a";

    /** Our reviewed corrections of quarter starts, each with its evidence. */
    static final String CORRECTIONS_FILE = "content/reviewed/hizb-quarter-corrections.csv";

    /** Its SHA-256 (2026-11-21). */
    static final String CORRECTIONS_SHA256 = "842987b2eba1ae152f176512599cc89796ef04a191e8f3b3ad4750baa5c2c138";

    /** The number of surahs. */
    private static final int SURAHS = 114;

    /** The number of ayahs in the Hafs / Kufan count. */
    private static final int AYAHS = 6236;

    /** The basmala is four words in the Tanzil text. */
    private static final int BASMALA_WORDS = 4;

    /** Rows written per database round trip. */
    private static final int BATCH = 500;

    /**
     * Created by Flyway.
     */
    public V1_1_1__ImportQuranText() {
        // no state
    }

    /**
     * Verifies, parses, checks and imports.
     *
     * @param context Flyway's context, giving the database connection of this migration's transaction
     * @throws Exception if a file is missing or changed, a count is wrong, or the database refuses a row — the
     *                   migration is then rolled back and the application does not start
     */
    @Override
    public void migrate(Context context) throws Exception {
        byte[] text = read(TEXT_FILE, TEXT_SHA256);
        byte[] meta = read(META_FILE, META_SHA256);
        byte[] corrections = read(CORRECTIONS_FILE, CORRECTIONS_SHA256);

        Map<Integer, Map<Integer, String>> ayahs = parseText(new String(text, StandardCharsets.UTF_8));
        Document xml = parseXml(meta);
        List<Element> surahs = elements(xml, "sura");
        List<int[]> pageStarts = starts(xml, "page");
        List<int[]> juzStarts = starts(xml, "juz");
        List<int[]> quarterStarts = applyCorrections(starts(xml, "quarter"),
                new String(corrections, StandardCharsets.UTF_8));
        Map<String, String> sajdahs = sajdahs(xml);

        check(surahs.size() == SURAHS, "surahs in the metadata: " + surahs.size());
        check(ayahs.size() == SURAHS, "surahs in the text: " + ayahs.size());
        check(pageStarts.size() == 604, "pages: " + pageStarts.size());
        check(juzStarts.size() == 30, "juz: " + juzStarts.size());
        check(quarterStarts.size() == 240, "quarters: " + quarterStarts.size());
        check(sajdahs.size() == 15, "sajdah places: " + sajdahs.size());
        int total = 0;
        for (Element surah : surahs) {
            int number = Integer.parseInt(surah.getAttribute("index"));
            int count = Integer.parseInt(surah.getAttribute("ayas"));
            Map<Integer, String> lines = ayahs.get(number);
            check(lines != null && lines.size() == count, "ayahs of surah " + number);
            for (int a = 1; a <= count; a++) {
                check(lines.containsKey(a), "ayah " + number + ":" + a);
            }
            total += count;
        }
        check(total == AYAHS, "ayahs: " + total);

        Connection connection = context.getConnection();
        insertSources(connection);
        insertSurahs(connection, surahs);
        insertAyahs(connection, ayahs, pageStarts, juzStarts, quarterStarts, sajdahs);
    }

    /**
     * Reads a source file from the application and verifies its SHA-256.
     *
     * @param path   the file's path on the class path
     * @param sha256 the recorded checksum
     * @return the file's bytes, unchanged
     * @throws IOException      if the file cannot be read
     * @throws IllegalStateException if the file is missing or its checksum differs
     */
    static byte[] read(String path, String sha256) throws IOException {
        try (InputStream in = V1_1_1__ImportQuranText.class.getClassLoader().getResourceAsStream(path)) {
            check(in != null, "missing source file " + path);
            byte[] bytes = in.readAllBytes();
            String actual = sha256(bytes);
            check(actual.equals(sha256), "checksum of " + path + " is " + actual + ", expected " + sha256);
            return bytes;
        }
    }

    /**
     * The SHA-256 of some bytes.
     *
     * @param bytes the bytes
     * @return 64 lower-case hexadecimal digits
     */
    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /**
     * Parses the Tanzil text: {@code surah|ayah|text} lines; comment lines ({@code #}) and empty lines are skipped.
     *
     * @param content the file's content
     * @return surah number → ayah number → the ayah's text, exactly as in the file
     */
    static Map<Integer, Map<Integer, String>> parseText(String content) {
        Map<Integer, Map<Integer, String>> ayahs = new TreeMap<>();
        for (String line : content.split("\n", -1)) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\|", 3);
            check(parts.length == 3, "a text line without surah|ayah|text");
            int surah = Integer.parseInt(parts[0]);
            int ayah = Integer.parseInt(parts[1]);
            String previous = ayahs.computeIfAbsent(surah, s -> new TreeMap<>()).put(ayah, parts[2]);
            check(previous == null, "ayah " + surah + ":" + ayah + " appears twice");
        }
        return ayahs;
    }

    /**
     * Parses the metadata file safely (no external entities, no DTD).
     *
     * @param bytes the file's bytes
     * @return the XML document
     * @throws ParserConfigurationException if the parser cannot be set up
     * @throws SAXException                 if the XML is malformed
     * @throws IOException                  if it cannot be read
     */
    static Document parseXml(byte[] bytes) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        try (InputStream in = new java.io.ByteArrayInputStream(bytes)) {
            return builder.parse(in);
        }
    }

    /**
     * All elements of a tag name, in document order.
     *
     * @param xml the document
     * @param tag the tag name
     * @return the elements
     */
    static List<Element> elements(Document xml, String tag) {
        NodeList nodes = xml.getElementsByTagName(tag);
        List<Element> list = new ArrayList<>(nodes.getLength());
        for (int i = 0; i < nodes.getLength(); i++) {
            list.add((Element) nodes.item(i));
        }
        return list;
    }

    /**
     * The starts of the pages, juz or quarters, in their order.
     *
     * @param xml the document
     * @param tag {@code page}, {@code juz} or {@code quarter}
     * @return pairs {surah, ayah}, the first entry being number 1
     */
    static List<int[]> starts(Document xml, String tag) {
        List<int[]> list = new ArrayList<>();
        for (Element e : elements(xml, tag)) {
            int index = Integer.parseInt(e.getAttribute("index"));
            check(index == list.size() + 1, tag + " " + index + " out of order");
            list.add(new int[] {Integer.parseInt(e.getAttribute("sura")), Integer.parseInt(e.getAttribute("aya"))});
        }
        return list;
    }

    /**
     * Applies the reviewed quarter corrections: every row must name the quarter's present start exactly, or the import
     * stops.
     *
     * @param quarters the quarter starts from the metadata
     * @param csv      the corrections file's content
     * @return the corrected quarter starts
     */
    static List<int[]> applyCorrections(List<int[]> quarters, String csv) {
        List<int[]> corrected = new ArrayList<>(quarters);
        for (String line : csv.split("\n")) {
            if (line.isBlank() || line.startsWith("#") || line.startsWith("quarter,")) {
                continue;
            }
            String[] f = line.split(",", 6);
            check(f.length == 6 && !f[5].isBlank(), "a correction row without its evidence");
            int quarter = Integer.parseInt(f[0]);
            int[] present = corrected.get(quarter - 1);
            check(present[0] == Integer.parseInt(f[1]) && present[1] == Integer.parseInt(f[2]),
                    "quarter " + quarter + " does not start at " + f[1] + ":" + f[2] + " in the metadata");
            corrected.set(quarter - 1, new int[] {Integer.parseInt(f[3]), Integer.parseInt(f[4])});
        }
        return corrected;
    }

    /**
     * The sajdah places and their type as the metadata gives it.
     *
     * @param xml the document
     * @return {@code "surah:ayah"} → {@code RECOMMENDED} or {@code OBLIGATORY}
     */
    static Map<String, String> sajdahs(Document xml) {
        Map<String, String> map = new LinkedHashMap<>();
        for (Element e : elements(xml, "sajda")) {
            String type = e.getAttribute("type");
            check(Set.of("recommended", "obligatory").contains(type), "unknown sajdah type " + type);
            map.put(e.getAttribute("sura") + ":" + e.getAttribute("aya"), type.toUpperCase(java.util.Locale.ROOT));
        }
        return map;
    }

    /**
     * Records the three source files.
     *
     * @param connection the migration's connection
     * @throws SQLException if a row is refused
     */
    private static void insertSources(Connection connection) throws SQLException {
        String sql = "insert into content_source (code, name, resource_path, sha256, licence, source_url, obtained_on) "
                + "values (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            addSource(ps, "TANZIL_UTHMANI", "Tanzil Quran Text (Uthmani, Version 1.1)", TEXT_FILE, TEXT_SHA256,
                    "CC BY 3.0 — verbatim copies only", "https://tanzil.net/download/", LocalDate.of(2026, 11, 17));
            addSource(ps, "TANZIL_METADATA", "Tanzil Quran Metadata 1.0", META_FILE, META_SHA256,
                    "CC BY 3.0", "https://tanzil.net/res/text/metadata/quran-data.xml", LocalDate.of(2026, 11, 17));
            addSource(ps, "QUARTER_CORRECTIONS", "Reviewed hizb-quarter corrections (tajweed-backend)",
                    CORRECTIONS_FILE, CORRECTIONS_SHA256, "MIT (this project)",
                    "https://github.com/Omar-Sherif-Sayed/tajweed-backend", LocalDate.of(2026, 11, 21));
            ps.executeBatch();
        }
    }

    /**
     * Adds one source row to the batch.
     *
     * @param ps       the prepared insert
     * @param code     the stable code
     * @param name     the source's name and version
     * @param path     the file's path in the application
     * @param sha256   its checksum
     * @param licence  its licence
     * @param url      where it was obtained
     * @param obtained the day it was obtained
     * @throws SQLException if the batch cannot take the row
     */
    private static void addSource(PreparedStatement ps, String code, String name, String path, String sha256,
                                  String licence, String url, LocalDate obtained) throws SQLException {
        ps.setString(1, code);
        ps.setString(2, name);
        ps.setString(3, path);
        ps.setString(4, sha256);
        ps.setString(5, licence);
        ps.setString(6, url);
        ps.setDate(7, Date.valueOf(obtained));
        ps.addBatch();
    }

    /**
     * Writes the 114 surahs.
     *
     * @param connection the migration's connection
     * @param surahs     the metadata's surah elements
     * @throws SQLException if a row is refused
     */
    private static void insertSurahs(Connection connection, List<Element> surahs) throws SQLException {
        String sql = "insert into surah (pk_id, name_ar, name_transliteration, name_en, revelation_type, "
                + "revelation_order, ayah_count) values (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Element s : surahs) {
                String type = s.getAttribute("type");
                check(type.equals("Meccan") || type.equals("Medinan"), "unknown revelation type " + type);
                ps.setShort(1, Short.parseShort(s.getAttribute("index")));
                ps.setString(2, s.getAttribute("name"));
                ps.setString(3, s.getAttribute("tname"));
                ps.setString(4, s.getAttribute("ename"));
                ps.setString(5, type.equals("Meccan") ? "MECCAN" : "MEDINAN");
                ps.setShort(6, Short.parseShort(s.getAttribute("order")));
                ps.setShort(7, Short.parseShort(s.getAttribute("ayas")));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Writes the 6,236 ayahs with their page, juz, quarter and sajdah, after checking the basmala of every first ayah.
     *
     * @param connection    the migration's connection
     * @param ayahs         the text, by surah and ayah
     * @param pageStarts    where each page starts
     * @param juzStarts     where each juz starts
     * @param quarterStarts where each (corrected) quarter starts
     * @param sajdahs       the sajdah places
     * @throws SQLException if a row is refused
     */
    private static void insertAyahs(Connection connection, Map<Integer, Map<Integer, String>> ayahs,
                                    List<int[]> pageStarts, List<int[]> juzStarts, List<int[]> quarterStarts,
                                    Map<String, String> sajdahs) throws SQLException {
        String basmala = letters(ayahs.get(1).get(1));
        String sql = "insert into ayah (pk_id, surah_id, number_in_surah, text_uthmani, basmala_word_count, page, juz, "
                + "hizb_quarter, sajdah_type) values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int id = 0;
        int page = 0;
        int juz = 0;
        int quarter = 0;
        int basmalas = 0;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Map.Entry<Integer, Map<Integer, String>> s : ayahs.entrySet()) {
                int surah = s.getKey();
                for (Map.Entry<Integer, String> a : s.getValue().entrySet()) {
                    int number = a.getKey();
                    String text = a.getValue();
                    id++;
                    page = advance(page, pageStarts, surah, number);
                    juz = advance(juz, juzStarts, surah, number);
                    quarter = advance(quarter, quarterStarts, surah, number);
                    int basmalaWords = 0;
                    if (number == 1 && surah != 1 && surah != 9) {
                        String[] words = text.split(" ");
                        check(words.length > BASMALA_WORDS
                                && letters(String.join(" ", java.util.Arrays.copyOf(words, BASMALA_WORDS)))
                                .equals(basmala), "surah " + surah + " does not begin with the basmala");
                        basmalaWords = BASMALA_WORDS;
                        basmalas++;
                    }
                    ps.setInt(1, id);
                    ps.setShort(2, (short) surah);
                    ps.setShort(3, (short) number);
                    ps.setString(4, text);
                    ps.setShort(5, (short) basmalaWords);
                    ps.setShort(6, (short) page);
                    ps.setShort(7, (short) juz);
                    ps.setShort(8, (short) quarter);
                    String sajdah = sajdahs.get(surah + ":" + number);
                    if (sajdah == null) {
                        ps.setNull(9, Types.VARCHAR);
                    } else {
                        ps.setString(9, sajdah);
                    }
                    ps.addBatch();
                    if (id % BATCH == 0) {
                        ps.executeBatch();
                    }
                }
            }
            ps.executeBatch();
        }
        check(id == AYAHS && page == 604 && juz == 30 && quarter == 240, "the division into pages, juz and quarters");
        check(basmalas == SURAHS - 2, "first ayahs with the basmala: " + basmalas);
    }

    /**
     * Moves to the next page, juz or quarter when the given ayah is where it starts.
     *
     * @param current the current number (0 before the first)
     * @param starts  where each one starts
     * @param surah   the ayah's surah
     * @param ayah    the ayah's number
     * @return the number the ayah belongs to
     */
    private static int advance(int current, List<int[]> starts, int surah, int ayah) {
        if (current < starts.size() && starts.get(current)[0] == surah && starts.get(current)[1] == ayah) {
            return current + 1;
        }
        return current;
    }

    /**
     * The letters of a text without its vowel marks — a comparison key only, never stored or shown. Used to see that
     * an ayah begins with the basmala although Tanzil writes 95:1 and 97:1 with a shadda («بِّسْمِ»).
     *
     * @param text Quran text
     * @return the text without combining marks and tatweel
     */
    static String letters(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        text.codePoints()
                .filter(c -> Character.getType(c) != Character.NON_SPACING_MARK && c != 'ـ')
                .forEach(sb::appendCodePoint);
        return sb.toString();
    }

    /**
     * Stops the import when a condition does not hold.
     *
     * @param condition what must be true
     * @param what      what was being checked, for the error message
     * @throws IllegalStateException if the condition is false
     */
    private static void check(boolean condition, String what) {
        if (!condition) {
            throw new IllegalStateException("Quran import stopped — check failed: " + what);
        }
    }
}
