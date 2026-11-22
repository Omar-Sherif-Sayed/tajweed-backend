package db.migration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Imports the reciter catalogue (F3, F4, F11; P1-D4): riwayat, reciters, recitations, the surahs each holds, the
 * audio sources in order, and the ayah timings — from mp3quran.net and EveryAyah, as chosen with the author on
 * 2026-11-22 ({@code docs/plan/backend-plan.md} §13 of the documentation repository).
 *
 * <p>The order of the work, as in the Quran import:
 * <ol>
 *   <li>every file is read from {@code src/main/resources/content/} and its SHA-256 compared with the value below —
 *       the sources' answers exactly as received, and our own reviewed files (the selection, the timing exclusions,
 *       the audio check);</li>
 *   <li>every row of the selection is checked against the sources: the reciter and the moshaf exist, the riwayah and
 *       the style are what the source says, every surah's audio file answered {@code 200} with audio on the check
 *       day, the EveryAyah folder exists with the bitrate it states;</li>
 *   <li>every timing file is checked against the Tanzil ayah count: ayahs 1 to the last (ayah 0 may come first),
 *       times that only go forward; a surah that fails is imported only if it is listed, with its reason, in
 *       {@code timing-exclusions.csv} — and a listed surah that does not fail stops the import too;</li>
 *   <li>only then are the rows written. Names and labels are the sources' own, never edited.</li>
 * </ol>
 * Timings are imported for Hafs recitations only: another riwayah counts the ayahs differently, so its timings would
 * not match the ayah numbers of the displayed text.
 */
public class V1_3_1__ImportRecitations extends BaseJavaMigration {

    /** mp3quran's reciters in Arabic ({@code /api/v3/reciters?language=ar}). */
    static final String RECITERS_AR = "content/mp3quran/reciters-ar.json";

    /** Its SHA-256. */
    static final String RECITERS_AR_SHA256 = "e883052abb0868fcfdf14a89bb3bcc25a419a1b70fe27e862af3e00d8e5a3a08";

    /** mp3quran's reciters in English ({@code /api/v3/reciters?language=eng}). */
    static final String RECITERS_EN = "content/mp3quran/reciters-en.json";

    /** Its SHA-256. */
    static final String RECITERS_EN_SHA256 = "1746ff7c10401ba3bce0328e9334696b0b682f45ca996d346af4760505ca5836";

    /** mp3quran's riwayat in Arabic ({@code /api/v3/riwayat?language=ar}). */
    static final String RIWAYAT_AR = "content/mp3quran/riwayat-ar.json";

    /** Its SHA-256. */
    static final String RIWAYAT_AR_SHA256 = "729481970579c75439dba2dcef2868f3e2edc285e4288f7b0e9b1226091b01b4";

    /** mp3quran's riwayat in English ({@code /api/v3/riwayat?language=eng}). */
    static final String RIWAYAT_EN = "content/mp3quran/riwayat-en.json";

    /** Its SHA-256. */
    static final String RIWAYAT_EN_SHA256 = "7632fbd6588b1a81669fd2019a5fe020855c249bcf0fcc03a90ff145bd276277";

    /** The recitations mp3quran has timings for ({@code /api/v3/ayat_timing/reads}). */
    static final String TIMING_READS = "content/mp3quran/ayat-timing-reads.json";

    /** Its SHA-256. */
    static final String TIMING_READS_SHA256 = "b22204b9af280a4bcfbbf9bfd44129a6744d6b3d97b14fed3306207df8ac7e78";

    /** The SHA-256 of every timing file ({@code timings/<read>/<surah>.json}, as received). */
    static final String TIMINGS_MANIFEST = "content/mp3quran/timings/SHA256SUMS";

    /** Its SHA-256. */
    static final String TIMINGS_MANIFEST_SHA256 = "65aacbecbb174180b8cf68b46b116c313e4a680833e7db857caa989ddd96d2a2";

    /** EveryAyah's own list of its folders ({@code everyayah.com/data/recitations.js}). */
    static final String EVERYAYAH = "content/everyayah/recitations.js";

    /** Its SHA-256. */
    static final String EVERYAYAH_SHA256 = "cde8cc62f7a6d830bd61934d279d8c4ab890d9db440087d1db51a9bd55864ee6";

    /** Our selection: the recitations chosen with the author, in the order shown. */
    static final String SELECTION = "content/reviewed/recitations.csv";

    /** Its SHA-256. */
    static final String SELECTION_SHA256 = "76ae598ed5b7176bb556d4f02dc3444c13a71d01a7c2e42f21e9aefb833341e1";

    /** Our reviewed list of surahs whose mp3quran timings fail the checks, each with its reason. */
    static final String EXCLUSIONS = "content/reviewed/timing-exclusions.csv";

    /** Its SHA-256. */
    static final String EXCLUSIONS_SHA256 = "4a02a15afd7079d320d70d20b6e8c0bf0949f369abaedcc86695616495b1b78b";

    /** The result of requesting every audio file once ({@code HEAD}) on the import day. */
    static final String AUDIO_CHECK = "content/reviewed/audio-check.csv";

    /** Its SHA-256. */
    static final String AUDIO_CHECK_SHA256 = "129511595f14866aee2cfd29dfa6124485ec8c87028e6ba9f4352ab94a8cbef0";

    /** mp3quran's id of Hafs an Asim — the riwayah of the shipped Tanzil text. */
    static final int HAFS = 1;

    /** The number of surahs. */
    private static final int SURAHS = 114;

    /** Rows per database round trip. */
    private static final int BATCH = 1000;

    /** The day the sources were read. */
    private static final LocalDate OBTAINED = LocalDate.of(2026, 11, 22);

    /** Reads the JSON files. */
    private static final JsonMapper JSON = JsonMapper.builder().build();

    /**
     * Created by Flyway.
     */
    public V1_3_1__ImportRecitations() {
        // no state
    }

    /**
     * Verifies, checks and imports.
     *
     * @param context Flyway's context, giving the connection of this migration's transaction
     * @throws Exception if a file is changed or a check fails — the migration is rolled back
     */
    @Override
    public void migrate(Context context) throws Exception {
        JsonNode recitersAr = JSON.readTree(V1_1_1__ImportQuranText.read(RECITERS_AR, RECITERS_AR_SHA256));
        JsonNode recitersEn = JSON.readTree(V1_1_1__ImportQuranText.read(RECITERS_EN, RECITERS_EN_SHA256));
        JsonNode riwayatAr = JSON.readTree(V1_1_1__ImportQuranText.read(RIWAYAT_AR, RIWAYAT_AR_SHA256));
        JsonNode riwayatEn = JSON.readTree(V1_1_1__ImportQuranText.read(RIWAYAT_EN, RIWAYAT_EN_SHA256));
        JsonNode reads = JSON.readTree(V1_1_1__ImportQuranText.read(TIMING_READS, TIMING_READS_SHA256));
        JsonNode everyAyah = JSON.readTree(V1_1_1__ImportQuranText.read(EVERYAYAH, EVERYAYAH_SHA256));
        Map<String, String> manifest = manifest(text(TIMINGS_MANIFEST, TIMINGS_MANIFEST_SHA256));
        List<String[]> selection = csv(text(SELECTION, SELECTION_SHA256), 10);
        Set<String> exclusions = new HashSet<>();
        for (String[] row : csv(text(EXCLUSIONS, EXCLUSIONS_SHA256), 3)) {
            check(!row[2].isBlank(), "an exclusion without its reason");
            exclusions.add(row[0] + "|" + row[1]);
        }
        Map<String, Boolean> audio = audioCheck(text(AUDIO_CHECK, AUDIO_CHECK_SHA256));

        Connection connection = context.getConnection();
        int[] ayahCounts = ayahCounts(connection);
        Map<Integer, JsonNode> reciterAr = byId(recitersAr.get("reciters"));
        Map<Integer, JsonNode> reciterEn = byId(recitersEn.get("reciters"));
        Map<Integer, JsonNode> riwayahAr = byId(riwayatAr.get("riwayat"));
        Map<Integer, JsonNode> riwayahEn = byId(riwayatEn.get("riwayat"));
        Set<Integer> timed = new HashSet<>();
        for (JsonNode r : reads) {
            timed.add(r.get("id").asInt());
        }
        Map<String, JsonNode> folders = new HashMap<>();
        for (Map.Entry<String, JsonNode> e : everyAyah.properties()) {
            if (e.getValue().has("subfolder")) {
                folders.put(e.getValue().get("subfolder").asString(), e.getValue());
            }
        }

        Map<Integer, Integer> reciterIds = new LinkedHashMap<>();
        Set<Integer> riwayatUsed = new TreeSet<>();
        List<Recitation> recitations = new ArrayList<>();
        int expectedId = 1;
        for (String[] row : selection) {
            check(Integer.parseInt(row[0]) == expectedId++, "the selection's ids run 1, 2, 3 … (row " + row[0] + ")");
            int reciter = Integer.parseInt(row[1]);
            check(reciterAr.containsKey(reciter) && reciterEn.containsKey(reciter), "mp3quran reciter " + reciter);
            reciterIds.putIfAbsent(reciter, reciterIds.size() + 1);
            Recitation r = row[2].equals("MP3QURAN")
                    ? fromMp3quran(row, reciterAr.get(reciter), reciterEn.get(reciter), riwayahAr, audio, timed)
                    : fromEveryAyah(row, folders, audio);
            r.reciterId = reciterIds.get(reciter);
            riwayatUsed.add(r.riwayahId);
            for (String folder : split(row[8])) {
                r.sources.add(everyAyahSource(folder, folders, audio));
            }
            recitations.add(r);
        }

        insertRiwayat(connection, riwayatUsed, riwayahAr, riwayahEn);
        insertReciters(connection, reciterIds, reciterAr, reciterEn);
        insertRecitations(connection, recitations, ayahCounts, manifest, exclusions);
        insertSources(connection);
    }

    /**
     * A recitation of the selection that is an mp3quran moshaf.
     *
     * @param row       the selection row
     * @param reciterAr the reciter in mp3quran's Arabic catalogue
     * @param reciterEn the reciter in mp3quran's English catalogue
     * @param riwayat   mp3quran's riwayat in Arabic, by id
     * @param audio     the audio check
     * @param timed     the moshafs mp3quran has timings for
     * @return the recitation, checked
     */
    private static Recitation fromMp3quran(String[] row, JsonNode reciterAr, JsonNode reciterEn,
                                           Map<Integer, JsonNode> riwayat, Map<String, Boolean> audio, Set<Integer> timed) {
        int moshafId = Integer.parseInt(row[3]);
        JsonNode ar = moshaf(reciterAr, moshafId);
        JsonNode en = moshaf(reciterEn, moshafId);
        Recitation r = new Recitation();
        r.id = Integer.parseInt(row[0]);
        r.provider = "MP3QURAN";
        r.moshafId = moshafId;
        r.nameAr = ar.get("name").asString();
        r.nameEn = en.get("name").asString();
        r.riwayahId = ar.get("rewaya_id").asInt();
        check(r.riwayahId == Integer.parseInt(row[4]), "the riwayah of moshaf " + moshafId);
        String riwayahName = riwayat.get(r.riwayahId).get("name").asString();
        check(r.nameAr.startsWith(riwayahName + " - "), "moshaf " + moshafId + " is named after its riwayah");
        r.style = styleOf(r.nameAr.substring(riwayahName.length() + 3));
        check(r.style.equals(row[5]), "the style of moshaf " + moshafId + " is «" + r.style + "»");
        String server = ar.get("server").asString();
        for (String s : ar.get("surah_list").asString().split(",")) {
            int surah = Integer.parseInt(s);
            check(Boolean.TRUE.equals(audio.get("MP3QURAN|" + moshafId + "|" + surah + "|0")),
                    "the audio file of surah " + surah + " of moshaf " + moshafId);
            r.surahs.add(surah);
        }
        check(r.surahs.size() == ar.get("surah_total").asInt(), "the surah count of moshaf " + moshafId);
        r.sources.add(new Source("SURAH_FILE", "MP3QURAN", server + "{surah}.mp3", null));
        r.timed = r.riwayahId == HAFS && timed.contains(moshafId);
        return r;
    }

    /**
     * A recitation of the selection that is one or more EveryAyah folders of the same recitation.
     *
     * @param row     the selection row
     * @param folders EveryAyah's folders
     * @param audio   the audio check
     * @return the recitation, checked
     */
    static Recitation fromEveryAyah(String[] row, Map<String, JsonNode> folders, Map<String, Boolean> audio) {
        check(row[2].equals("EVERYAYAH"), "a provider MP3QURAN or EVERYAYAH (row " + row[0] + ")");
        check(!row[6].isBlank() && !row[7].isBlank(), "the label of EveryAyah row " + row[0]);
        Recitation r = new Recitation();
        r.id = Integer.parseInt(row[0]);
        r.provider = "EVERYAYAH";
        r.nameAr = row[6];
        r.nameEn = row[7];
        r.riwayahId = Integer.parseInt(row[4]);
        check(r.riwayahId == HAFS, "EveryAyah's folders without a riwayah in their name are Hafs (row " + row[0] + ")");
        r.style = row[5];
        for (String folder : split(row[3])) {
            r.sources.add(everyAyahSource(folder, folders, audio));
        }
        check(!r.sources.isEmpty(), "the folders of EveryAyah row " + row[0]);
        for (int s = 1; s <= SURAHS; s++) {
            r.surahs.add(s);
        }
        r.timed = false;
        return r;
    }

    /**
     * An EveryAyah folder as an audio source, after checking that it exists with its bitrate and that the first and
     * the last ayah of every surah answered with audio.
     *
     * @param folder  the folder's name
     * @param folders EveryAyah's folders
     * @param audio   the audio check
     * @return the source
     */
    private static Source everyAyahSource(String folder, Map<String, JsonNode> folders, Map<String, Boolean> audio) {
        JsonNode f = folders.get(folder);
        check(f != null, "EveryAyah folder " + folder + " in its own list");
        String bitrate = f.get("bitrate").asString().toLowerCase(Locale.ROOT);
        check(bitrate.matches("\\d+kbps"), "the bitrate of " + folder + " (EveryAyah writes «64kbps» or «64Kbps»)");
        int checked = 0;
        for (Map.Entry<String, Boolean> e : audio.entrySet()) {
            if (e.getKey().startsWith("EVERYAYAH|" + folder + "|")) {
                check(e.getValue(), "EveryAyah file " + e.getKey());
                checked++;
            }
        }
        check(checked >= SURAHS, "the first and last ayah of every surah of " + folder + " were checked");
        return new Source("AYAH_FILE", "EVERYAYAH", "https://everyayah.com/data/" + folder + "/{surah}{ayah}.mp3",
                Integer.parseInt(bitrate.replace("kbps", "")));
    }

    /**
     * The style a moshaf's label names after its riwayah.
     *
     * @param label the part of mp3quran's label after «riwayah - »
     * @return {@code MURATTAL}, {@code MUJAWWAD}, {@code MUALLIM}, or empty when the label names no style
     */
    static String styleOf(String label) {
        return switch (label) {
            case "مرتل" -> "MURATTAL";
            case "المصحف المجود" -> "MUJAWWAD";
            case "المصحف المعلم" -> "MUALLIM";
            default -> "";
        };
    }

    /**
     * Checks one surah's timing file: ayahs 1 to the last of the Tanzil count, ayah 0 allowed before them, every ayah
     * with an end after its start, every start at or after the one before.
     *
     * @param rows     the file's rows: {ayah, start, end}
     * @param ayahs    the surah's number of ayahs
     * @return {@code null} when it passes, otherwise what is wrong
     */
    static String timingProblem(List<int[]> rows, int ayahs) {
        int first = rows.isEmpty() ? -1 : rows.getFirst()[0];
        int expected = first == 0 ? 0 : 1;
        if (rows.size() != ayahs + 1 - expected) {
            return rows.size() + " rows for " + ayahs + " ayahs";
        }
        int previousStart = -1;
        for (int[] row : rows) {
            if (row[0] != expected++) {
                return "ayah " + row[0] + " out of order";
            }
            if (row[1] < 0 || row[2] <= row[1] || row[1] < previousStart) {
                return "the times of ayah " + row[0];
            }
            previousStart = row[1];
        }
        return null;
    }

    /**
     * Writes the riwayat the selection uses, with their rawi and qari taken from mp3quran's name «rawi عن qari».
     *
     * @param connection the migration's connection
     * @param ids        the riwayat used
     * @param ar         mp3quran's riwayat in Arabic
     * @param en         mp3quran's riwayat in English
     * @throws SQLException if a row is refused
     */
    static void insertRiwayat(Connection connection, Set<Integer> ids, Map<Integer, JsonNode> ar,
                                      Map<Integer, JsonNode> en) throws SQLException {
        String sql = "insert into riwayah (pk_id, name_ar, name_en, rawi_ar, qari_ar, matches_displayed_text) "
                + "values (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int id : ids) {
                String nameAr = ar.get(id).get("name").asString();
                String[] parts = nameAr.split(" عن ", 2);
                check(parts.length == 2, "riwayah «" + nameAr + "» is named «rawi عن qari»");
                check(id != HAFS || nameAr.equals("حفص عن عاصم"), "mp3quran's riwayah 1 is Hafs an Asim");
                ps.setShort(1, (short) id);
                ps.setString(2, nameAr);
                ps.setString(3, en.get(id).get("name").asString().strip());
                ps.setString(4, parts[0].strip());
                ps.setString(5, parts[1].strip());
                ps.setBoolean(6, id == HAFS);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Writes the reciters, in the order of the selection, with mp3quran's names.
     *
     * @param connection the migration's connection
     * @param ids        mp3quran id → our id
     * @param ar         mp3quran's reciters in Arabic, by id
     * @param en         mp3quran's reciters in English, by id
     * @throws SQLException if a row is refused
     */
    private static void insertReciters(Connection connection, Map<Integer, Integer> ids, Map<Integer, JsonNode> ar,
                                       Map<Integer, JsonNode> en) throws SQLException {
        String sql = "insert into reciter (pk_id, name_ar, name_en, mp3quran_id) values (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Map.Entry<Integer, Integer> e : ids.entrySet()) {
                ps.setShort(1, e.getValue().shortValue());
                ps.setString(2, ar.get(e.getKey()).get("name").asString().strip());
                ps.setString(3, en.get(e.getKey()).get("name").asString().strip());
                ps.setInt(4, e.getKey());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Writes the recitations, their surahs, their sources and their timings.
     *
     * @param connection the migration's connection
     * @param list       the checked recitations
     * @param ayahCounts the Tanzil ayah count of every surah (index = surah)
     * @param manifest   the SHA-256 of every timing file
     * @param exclusions the reviewed failing surahs, as {@code "read|surah"}
     * @throws SQLException if a row is refused
     * @throws IOException  if a timing file cannot be read
     */
    static void insertRecitations(Connection connection, List<Recitation> list, int[] ayahCounts,
                                          Map<String, String> manifest, Set<String> exclusions)
            throws SQLException, IOException {
        Set<String> excludedSeen = new HashSet<>();
        String rec = "insert into recitation (pk_id, reciter_id, riwayah_id, style, name_ar, name_en, provider, "
                + "mp3quran_moshaf_id, surah_count, has_timings) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String sur = "insert into recitation_surah (recitation_id, surah_id) values (?, ?)";
        String src = "insert into audio_source (recitation_id, priority, kind, provider, url_template, bitrate_kbps, "
                + "download_allowed) values (?, ?, ?, ?, ?, ?, false)";
        String tim = "insert into ayah_timing (recitation_id, surah_id, ayah_number, start_ms, end_ms) "
                + "values (?, ?, ?, ?, ?)";
        try (PreparedStatement pr = connection.prepareStatement(rec);
             PreparedStatement ps = connection.prepareStatement(sur);
             PreparedStatement pa = connection.prepareStatement(src);
             PreparedStatement pt = connection.prepareStatement(tim)) {
            int timingRows = 0;
            for (Recitation r : list) {
                List<List<int[]>> timings = new ArrayList<>();
                List<Integer> timedSurahs = new ArrayList<>();
                String whole = r.moshafId + "|*";
                if (r.timed && exclusions.contains(whole)) {
                    int failing = 0;
                    for (int surah : r.surahs) {
                        String path = String.format("content/mp3quran/timings/%d/%03d.json", r.moshafId, surah);
                        String sha = manifest.get(path.substring("content/mp3quran/timings/".length()));
                        check(sha != null, "timing file " + path + " in the manifest");
                        if (timingProblem(timingRows(JSON.readTree(V1_1_1__ImportQuranText.read(path, sha))),
                                ayahCounts[surah]) != null) {
                            failing++;
                        }
                    }
                    check(failing * 2 > r.surahs.size(), "a recitation excluded whole (" + whole
                            + ") fails in most of its surahs — " + failing + " of " + r.surahs.size());
                    excludedSeen.add(whole);
                } else if (r.timed) {
                    for (int surah : r.surahs) {
                        String path = String.format("content/mp3quran/timings/%d/%03d.json", r.moshafId, surah);
                        String sha = manifest.get(path.substring("content/mp3quran/timings/".length()));
                        check(sha != null, "timing file " + path + " in the manifest");
                        List<int[]> rows = timingRows(JSON.readTree(V1_1_1__ImportQuranText.read(path, sha)));
                        String problem = timingProblem(rows, ayahCounts[surah]);
                        String key = r.moshafId + "|" + surah;
                        if (exclusions.contains(key)) {
                            check(problem != null, "excluded timings " + key + " pass the checks — remove the exclusion");
                            excludedSeen.add(key);
                            continue;
                        }
                        check(problem == null, "the timings of moshaf " + r.moshafId + ", surah " + surah + ": " + problem);
                        timings.add(rows);
                        timedSurahs.add(surah);
                    }
                }
                pr.setShort(1, (short) r.id);
                pr.setShort(2, (short) r.reciterId);
                pr.setShort(3, (short) r.riwayahId);
                if (r.style.isEmpty()) {
                    pr.setNull(4, Types.VARCHAR);
                } else {
                    pr.setString(4, r.style);
                }
                pr.setString(5, r.nameAr);
                pr.setString(6, r.nameEn);
                pr.setString(7, r.provider);
                if (r.moshafId == 0) {
                    pr.setNull(8, Types.INTEGER);
                } else {
                    pr.setInt(8, r.moshafId);
                }
                pr.setShort(9, (short) r.surahs.size());
                pr.setBoolean(10, !timedSurahs.isEmpty());
                pr.executeUpdate();
                for (int surah : r.surahs) {
                    ps.setShort(1, (short) r.id);
                    ps.setShort(2, (short) surah);
                    ps.addBatch();
                }
                ps.executeBatch();
                int priority = 1;
                for (Source s : r.sources) {
                    pa.setShort(1, (short) r.id);
                    pa.setShort(2, (short) priority++);
                    pa.setString(3, s.kind);
                    pa.setString(4, s.provider);
                    pa.setString(5, s.urlTemplate);
                    if (s.bitrate == null) {
                        pa.setNull(6, Types.SMALLINT);
                    } else {
                        pa.setShort(6, s.bitrate.shortValue());
                    }
                    pa.addBatch();
                }
                pa.executeBatch();
                for (int i = 0; i < timedSurahs.size(); i++) {
                    for (int[] row : timings.get(i)) {
                        pt.setShort(1, (short) r.id);
                        pt.setShort(2, timedSurahs.get(i).shortValue());
                        pt.setShort(3, (short) row[0]);
                        pt.setInt(4, row[1]);
                        pt.setInt(5, row[2]);
                        pt.addBatch();
                        if (++timingRows % BATCH == 0) {
                            pt.executeBatch();
                        }
                    }
                }
            }
            pt.executeBatch();
        }
        check(excludedSeen.equals(exclusions), "every exclusion names a timed surah of the selection");
    }

    /**
     * Records the files of this import.
     *
     * @param connection the migration's connection
     * @throws SQLException if a row is refused
     */
    private static void insertSources(Connection connection) throws SQLException {
        String sql = "insert into content_source (code, name, resource_path, sha256, licence, source_url, obtained_on) "
                + "values (?, ?, ?, ?, ?, ?, ?)";
        String mp3quran = "mp3quran.net: «جميع الحقوق متاحة للجميع» (mp3quran.net/ar/privacy)";
        String[][] rows = {
            {"MP3QURAN_CATALOGUE", "mp3quran.net API v3 — reciters (Arabic)", RECITERS_AR, RECITERS_AR_SHA256, mp3quran,
                "https://www.mp3quran.net/api/v3/reciters?language=ar"},
            {"MP3QURAN_CATALOGUE_EN", "mp3quran.net API v3 — reciters (English)", RECITERS_EN, RECITERS_EN_SHA256, mp3quran,
                "https://www.mp3quran.net/api/v3/reciters?language=eng"},
            {"MP3QURAN_RIWAYAT", "mp3quran.net API v3 — riwayat (Arabic)", RIWAYAT_AR, RIWAYAT_AR_SHA256, mp3quran,
                "https://www.mp3quran.net/api/v3/riwayat?language=ar"},
            {"MP3QURAN_RIWAYAT_EN", "mp3quran.net API v3 — riwayat (English)", RIWAYAT_EN, RIWAYAT_EN_SHA256, mp3quran,
                "https://www.mp3quran.net/api/v3/riwayat?language=eng"},
            {"MP3QURAN_TIMING_READS", "mp3quran.net API v3 — recitations with ayah timings", TIMING_READS,
                TIMING_READS_SHA256, mp3quran, "https://www.mp3quran.net/api/v3/ayat_timing/reads"},
            {"MP3QURAN_TIMINGS", "mp3quran.net API v3 — ayah timings (manifest of the files)", TIMINGS_MANIFEST,
                TIMINGS_MANIFEST_SHA256, mp3quran, "https://www.mp3quran.net/api/v3/ayat_timing"},
            {"EVERYAYAH_LIST", "EveryAyah — recitations.js", EVERYAYAH, EVERYAYAH_SHA256,
                "No published terms; streaming only until EveryAyah answers (request #20971)",
                "https://everyayah.com/data/recitations.js"},
            {"RECITATION_SELECTION", "Recitations chosen with the author (tajweed-backend)", SELECTION, SELECTION_SHA256,
                "MIT (this project)", "https://github.com/Omar-Sherif-Sayed/tajweed-backend"},
            {"TIMING_EXCLUSIONS", "Reviewed mp3quran timings left out (tajweed-backend)", EXCLUSIONS, EXCLUSIONS_SHA256,
                "MIT (this project)", "https://github.com/Omar-Sherif-Sayed/tajweed-backend"},
            {"AUDIO_CHECK", "Every audio file requested once on 2026-11-22 (tajweed-backend)", AUDIO_CHECK,
                AUDIO_CHECK_SHA256, "MIT (this project)", "https://github.com/Omar-Sherif-Sayed/tajweed-backend"},
        };
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (String[] r : rows) {
                for (int i = 0; i < r.length; i++) {
                    ps.setString(i + 1, r[i]);
                }
                ps.setDate(7, Date.valueOf(OBTAINED));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * The rows of one timing file: only the ayah number and its times are taken (mp3quran's page drawings are not).
     *
     * @param json the file
     * @return {ayah, start ms, end ms}, in the file's order
     */
    static List<int[]> timingRows(JsonNode json) {
        List<int[]> rows = new ArrayList<>();
        for (JsonNode r : json) {
            rows.add(new int[] {r.get("ayah").asInt(), r.get("start_time").asInt(), r.get("end_time").asInt()});
        }
        return rows;
    }

    /**
     * A moshaf of a reciter.
     *
     * @param reciter the reciter's entry
     * @param id      the moshaf id
     * @return the moshaf's entry
     */
    private static JsonNode moshaf(JsonNode reciter, int id) {
        for (JsonNode m : reciter.get("moshaf")) {
            if (m.get("id").asInt() == id) {
                return m;
            }
        }
        throw new IllegalStateException("Recitation import stopped — check failed: moshaf " + id + " of reciter "
                + reciter.get("id").asInt());
    }

    /**
     * The entries of a JSON array, by their {@code id}.
     *
     * @param array the array
     * @return id → entry
     */
    static Map<Integer, JsonNode> byId(JsonNode array) {
        Map<Integer, JsonNode> map = new HashMap<>();
        for (JsonNode n : array) {
            map.put(n.get("id").asInt(), n);
        }
        return map;
    }

    /**
     * The Tanzil ayah count of every surah, from the imported {@code surah} table.
     *
     * @param connection the migration's connection
     * @return index = surah number
     * @throws SQLException if it cannot be read
     */
    private static int[] ayahCounts(Connection connection) throws SQLException {
        int[] counts = new int[SURAHS + 1];
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("select pk_id, ayah_count from surah")) {
            while (rs.next()) {
                counts[rs.getInt(1)] = rs.getInt(2);
            }
        }
        return counts;
    }

    /**
     * The audio check: {@code provider,key,surah,ayah,status,bytes,content_type} per file.
     *
     * @param csv the file's content
     * @return {@code "provider|key|surah|ayah"} → whether it answered {@code 200} with audio
     */
    static Map<String, Boolean> audioCheck(String csv) {
        Map<String, Boolean> map = new HashMap<>();
        for (String[] r : csv(csv, 7)) {
            map.put(r[0] + "|" + r[1] + "|" + r[2] + "|" + r[3], r[4].equals("200") && r[6].startsWith("audio/"));
        }
        return map;
    }

    /**
     * The timing manifest: {@code <sha256>  <read>/<surah>.json} per line.
     *
     * @param text the file's content
     * @return path → SHA-256
     */
    static Map<String, String> manifest(String text) {
        Map<String, String> map = new HashMap<>();
        for (String line : text.split("\n")) {
            if (!line.isBlank()) {
                String[] p = line.strip().split("\\s+", 2);
                check(p.length == 2 && p[0].matches("[0-9a-f]{64}"), "a manifest line");
                map.put(p[1], p[0]);
            }
        }
        return map;
    }

    /**
     * The rows of one of our CSV files: comment lines ({@code #}) and the header (the first other line) skipped.
     * Values hold no comma.
     *
     * @param text    the file's content
     * @param columns how many columns each row must have
     * @return the rows
     */
    static List<String[]> csv(String text, int columns) {
        List<String[]> rows = new ArrayList<>();
        boolean header = true;
        for (String line : text.split("\n")) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            if (header) {
                header = false;
                continue;
            }
            String[] r = line.split(",", -1);
            check(r.length == columns, "a row of " + columns + " columns: " + line);
            rows.add(r);
        }
        return rows;
    }

    /**
     * A list written with {@code ;} between its items.
     *
     * @param value the value, possibly empty
     * @return the items
     */
    private static List<String> split(String value) {
        List<String> list = new ArrayList<>();
        for (String s : value.split(";")) {
            if (!s.isBlank()) {
                list.add(s.strip());
            }
        }
        return list;
    }

    /**
     * A verified file as UTF-8 text.
     *
     * @param path   the file
     * @param sha256 its recorded checksum
     * @return its content
     * @throws IOException if it cannot be read
     */
    static String text(String path, String sha256) throws IOException {
        return new String(V1_1_1__ImportQuranText.read(path, sha256), StandardCharsets.UTF_8);
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
            throw new IllegalStateException("Recitation import stopped — check failed: " + what);
        }
    }

    /**
     * A recitation being imported.
     */
    static final class Recitation {

        /** Our id. */
        int id;

        /** Our reciter id. */
        int reciterId;

        /** mp3quran's riwayah id. */
        int riwayahId;

        /** The style's name, or empty. */
        String style;

        /** The source's label in Arabic. */
        String nameAr;

        /** The source's label in English. */
        String nameEn;

        /** {@code MP3QURAN} or {@code EVERYAYAH}. */
        String provider;

        /** mp3quran's moshaf id, or 0. */
        int moshafId;

        /** Whether mp3quran's timings are imported for it. */
        boolean timed;

        /** The surahs it holds, in order. */
        final List<Integer> surahs = new ArrayList<>();

        /** Its sources, in order. */
        final List<Source> sources = new ArrayList<>();
    }

    /**
     * An audio source being imported.
     *
     * @param kind        {@code SURAH_FILE} or {@code AYAH_FILE}
     * @param provider    {@code MP3QURAN} or {@code EVERYAYAH}
     * @param urlTemplate the address template
     * @param bitrate     the bitrate the source states, or {@code null}
     */
    private record Source(String kind, String provider, String urlTemplate, Integer bitrate) {
    }
}
