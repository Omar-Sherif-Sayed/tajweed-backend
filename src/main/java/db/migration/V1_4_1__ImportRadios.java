package db.migration;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Imports the radio stations (F7): mp3quran's channels, as decided with the author on 2026-11-22 — recitation only
 * ({@code docs/plan/backend-plan.md} §14 of the documentation repository).
 *
 * <ol>
 *   <li>mp3quran's lists (Arabic and English) and our two reviewed files are read and their SHA-256 compared;</li>
 *   <li>every channel of mp3quran's list must have a decision in {@code radios.csv}, and every decision a channel —
 *       a channel added by mp3quran stops the import until it is decided;</li>
 *   <li>a kept channel must have answered with audio on the check day ({@code radio-check.csv}), its reciter must be
 *       in mp3quran's catalogue, and its riwayah in mp3quran's list;</li>
 *   <li>only then are the rows written: names as mp3quran writes them; a channel of one of our reciters linked to
 *       him; a riwayah only where the channel names it (missing riwayat are added from mp3quran's list).</li>
 * </ol>
 */
public class V1_4_1__ImportRadios extends BaseJavaMigration {

    /** mp3quran's radios in Arabic ({@code /api/v3/radios?language=ar}). */
    static final String RADIOS_AR = "content/mp3quran/radios-ar.json";

    /** Its SHA-256. */
    static final String RADIOS_AR_SHA256 = "f99a1458914a30598a5304f1ad291b3e9c5b075889849f66bae8aedc0079487a";

    /** mp3quran's radios in English ({@code /api/v3/radios?language=eng}). */
    static final String RADIOS_EN = "content/mp3quran/radios-en.json";

    /** Its SHA-256. */
    static final String RADIOS_EN_SHA256 = "2d173b944963f3ebf45a764029857130ef7f290dcb09570365c65c400686f25a";

    /** Our decision for every channel. */
    static final String DECISIONS = "content/reviewed/radios.csv";

    /** Its SHA-256. */
    static final String DECISIONS_SHA256 = "7354848d69fe696662d26928b6674307552a74dfdc0589ec47f8648117dfcd7c";

    /** Every kept stream requested on the import day. */
    static final String CHECK = "content/reviewed/radio-check.csv";

    /** Its SHA-256. */
    static final String CHECK_SHA256 = "e7e10d30acc6fd0ba465d5b4860423f091bcf003b0a41a29f7d3f9bbaf3768d1";

    /** The day the sources were read. */
    private static final LocalDate OBTAINED = LocalDate.of(2026, 11, 22);

    /** Reads the JSON files. */
    private static final JsonMapper JSON = JsonMapper.builder().build();

    /**
     * Created by Flyway.
     */
    public V1_4_1__ImportRadios() {
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
        Map<Integer, JsonNode> ar = V1_3_1__ImportRecitations.byId(
                JSON.readTree(V1_1_1__ImportQuranText.read(RADIOS_AR, RADIOS_AR_SHA256)).get("radios"));
        Map<Integer, JsonNode> en = V1_3_1__ImportRecitations.byId(
                JSON.readTree(V1_1_1__ImportQuranText.read(RADIOS_EN, RADIOS_EN_SHA256)).get("radios"));
        Map<Integer, JsonNode> reciters = V1_3_1__ImportRecitations.byId(JSON.readTree(V1_1_1__ImportQuranText.read(
                V1_3_1__ImportRecitations.RECITERS_AR, V1_3_1__ImportRecitations.RECITERS_AR_SHA256)).get("reciters"));
        Map<Integer, JsonNode> riwayatAr = V1_3_1__ImportRecitations.byId(JSON.readTree(V1_1_1__ImportQuranText.read(
                V1_3_1__ImportRecitations.RIWAYAT_AR, V1_3_1__ImportRecitations.RIWAYAT_AR_SHA256)).get("riwayat"));
        Map<Integer, JsonNode> riwayatEn = V1_3_1__ImportRecitations.byId(JSON.readTree(V1_1_1__ImportQuranText.read(
                V1_3_1__ImportRecitations.RIWAYAT_EN, V1_3_1__ImportRecitations.RIWAYAT_EN_SHA256)).get("riwayat"));
        List<String[]> decisions = V1_3_1__ImportRecitations.csv(
                V1_3_1__ImportRecitations.text(DECISIONS, DECISIONS_SHA256), 5);
        Map<Integer, Boolean> answered = new HashMap<>();
        for (String[] r : V1_3_1__ImportRecitations.csv(V1_3_1__ImportRecitations.text(CHECK, CHECK_SHA256), 4)) {
            answered.put(Integer.parseInt(r[0]), r[1].equals("200") && r[3].startsWith("audio/"));
        }

        check(ar.keySet().equals(en.keySet()), "the Arabic and English lists hold the same channels");
        Set<Integer> decided = new HashSet<>();
        for (String[] d : decisions) {
            check(decided.add(Integer.parseInt(d[0])), "one decision per channel (" + d[0] + ")");
        }
        check(decided.equals(ar.keySet()), "a decision for every channel of mp3quran's list, and no other");

        Connection connection = context.getConnection();
        Map<Integer, Integer> ourReciters = ourReciters(connection);
        Set<Integer> riwayatNeeded = new TreeSet<>();
        for (String[] d : decisions) {
            if (!d[3].isBlank()) {
                riwayatNeeded.add(Integer.parseInt(d[3]));
            }
        }
        Set<Integer> missing = new TreeSet<>(riwayatNeeded);
        missing.removeAll(existingRiwayat(connection));
        for (int id : missing) {
            check(riwayatAr.containsKey(id) && riwayatEn.containsKey(id), "mp3quran riwayah " + id);
        }
        V1_3_1__ImportRecitations.insertRiwayat(connection, missing, riwayatAr, riwayatEn);

        String station = "insert into radio_station (pk_id, provider, provider_key, kind, reciter_id, riwayah_id, "
                + "name_ar, name_en, country_code, broadcaster_url, sort_order) "
                + "values (?, 'MP3QURAN', ?, ?, ?, ?, ?, ?, null, null, ?)";
        String stream = "insert into radio_stream (station_id, url, priority) values (?, ?, 1)";
        int id = 0;
        try (PreparedStatement ps = connection.prepareStatement(station);
             PreparedStatement pu = connection.prepareStatement(stream)) {
            for (String[] d : decisions) {
                int key = Integer.parseInt(d[0]);
                String kind = d[1];
                if (kind.equals("LEFT_OUT")) {
                    check(!d[4].isBlank(), "a channel left out with its reason (" + key + ")");
                    continue;
                }
                check(kind.equals("RECITER") || kind.equals("RECITERS"), "a decision RECITER, RECITERS or LEFT_OUT ("
                        + key + ")");
                check(Boolean.TRUE.equals(answered.get(key)), "channel " + key + " answered with audio on the check day");
                String url = ar.get(key).get("url").asString();
                check(url.equals(en.get(key).get("url").asString()) && url.startsWith("https://"),
                        "channel " + key + " has one https address in both lists");
                Integer reciter = d[2].isBlank() ? null : Integer.parseInt(d[2]);
                check(reciter == null || (kind.equals("RECITER") && reciters.containsKey(reciter)),
                        "the reciter of channel " + key + " is in mp3quran's catalogue");
                id++;
                ps.setShort(1, (short) id);
                ps.setInt(2, key);
                ps.setString(3, kind);
                Integer ours = reciter == null ? null : ourReciters.get(reciter);
                if (ours == null) {
                    ps.setNull(4, Types.SMALLINT);
                } else {
                    ps.setShort(4, ours.shortValue());
                }
                if (d[3].isBlank()) {
                    ps.setNull(5, Types.SMALLINT);
                } else {
                    ps.setShort(5, Short.parseShort(d[3]));
                }
                ps.setString(6, ar.get(key).get("name").asString().strip());
                ps.setString(7, en.get(key).get("name").asString().strip());
                ps.setShort(8, (short) id);
                ps.addBatch();
                pu.setShort(1, (short) id);
                pu.setString(2, url);
                pu.addBatch();
            }
            ps.executeBatch();
            pu.executeBatch();
        }
        check(id > 0, "at least one station kept");
        insertSources(connection);
    }

    /**
     * Our reciters, by mp3quran's id.
     *
     * @param connection the migration's connection
     * @return mp3quran id → our id
     * @throws SQLException if it cannot be read
     */
    private static Map<Integer, Integer> ourReciters(Connection connection) throws SQLException {
        Map<Integer, Integer> map = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement("select mp3quran_id, pk_id from reciter");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getInt(1), rs.getInt(2));
            }
        }
        return map;
    }

    /**
     * The riwayat already imported.
     *
     * @param connection the migration's connection
     * @return their ids
     * @throws SQLException if it cannot be read
     */
    private static Set<Integer> existingRiwayat(Connection connection) throws SQLException {
        Set<Integer> ids = new HashSet<>();
        try (PreparedStatement ps = connection.prepareStatement("select pk_id from riwayah");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
        }
        return ids;
    }

    /**
     * Records the files of this import.
     *
     * @param connection the migration's connection
     * @throws SQLException if a row is refused
     */
    private static void insertSources(Connection connection) throws SQLException {
        String mp3quran = "mp3quran.net: «جميع الحقوق متاحة للجميع» (mp3quran.net/ar/privacy)";
        String ours = "MIT (this project)";
        String repo = "https://github.com/Omar-Sherif-Sayed/tajweed-backend";
        String[][] rows = {
            {"MP3QURAN_RADIOS", "mp3quran.net API v3 — radios (Arabic)", RADIOS_AR, RADIOS_AR_SHA256, mp3quran,
                "https://www.mp3quran.net/api/v3/radios?language=ar"},
            {"MP3QURAN_RADIOS_EN", "mp3quran.net API v3 — radios (English)", RADIOS_EN, RADIOS_EN_SHA256, mp3quran,
                "https://www.mp3quran.net/api/v3/radios?language=eng"},
            {"RADIO_DECISIONS", "The radio channels kept or left out, with reasons (tajweed-backend)", DECISIONS,
                DECISIONS_SHA256, ours, repo},
            {"RADIO_CHECK", "Every kept radio stream requested on 2026-11-22 (tajweed-backend)", CHECK, CHECK_SHA256,
                ours, repo},
        };
        try (PreparedStatement ps = connection.prepareStatement("insert into content_source (code, name, resource_path, "
                + "sha256, licence, source_url, obtained_on) values (?, ?, ?, ?, ?, ?, ?)")) {
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
     * Stops the import when a condition does not hold.
     *
     * @param condition what must be true
     * @param what      what was being checked
     * @throws IllegalStateException if the condition is false
     */
    private static void check(boolean condition, String what) {
        if (!condition) {
            throw new IllegalStateException("Radio import stopped — check failed: " + what);
        }
    }
}
