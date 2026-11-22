package db.migration;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adds the recitations of {@code content/reviewed/recitations-added.csv} — today one: EveryAyah's folders named only
 * «Hudhaify», which the author identified on 2026-11-22 as Sheikh Ali al-Hudhaifi ({@code docs/plan/backend-plan.md}
 * §14 of the documentation repository).
 *
 * <p>The rows go through the very checks of {@link V1_3_1__ImportRecitations} — the folder exists in EveryAyah's own
 * list with its bitrate, the first and last ayah of every surah answered with audio on the check day — and the reciter
 * must already be in the catalogue. The recording cannot be compared with mp3quran's (Ali al-Hudhaifi's timings there
 * fail the ayah count), so it is its own recitation, never a fallback.
 */
public class V1_3_2__AddHudhaifyRecitation extends BaseJavaMigration {

    /** The recitations added after the first import. */
    static final String ADDED = "content/reviewed/recitations-added.csv";

    /** Its SHA-256. */
    static final String ADDED_SHA256 = "13afa3b27e9e42c66bc53de9cc23ed638a807fcea55730b081092d00635b9c77";

    /** The day the file was written. */
    private static final LocalDate OBTAINED = LocalDate.of(2026, 11, 22);

    /** Reads EveryAyah's list. */
    private static final JsonMapper JSON = JsonMapper.builder().build();

    /**
     * Created by Flyway.
     */
    public V1_3_2__AddHudhaifyRecitation() {
        // no state
    }

    /**
     * Verifies, checks and adds.
     *
     * @param context Flyway's context, giving the connection of this migration's transaction
     * @throws Exception if a file is changed or a check fails — the migration is rolled back
     */
    @Override
    public void migrate(Context context) throws Exception {
        List<String[]> rows = V1_3_1__ImportRecitations.csv(
                V1_3_1__ImportRecitations.text(ADDED, ADDED_SHA256), 10);
        JsonNode everyAyah = JSON.readTree(V1_1_1__ImportQuranText.read(
                V1_3_1__ImportRecitations.EVERYAYAH, V1_3_1__ImportRecitations.EVERYAYAH_SHA256));
        Map<String, Boolean> audio = V1_3_1__ImportRecitations.audioCheck(V1_3_1__ImportRecitations.text(
                V1_3_1__ImportRecitations.AUDIO_CHECK, V1_3_1__ImportRecitations.AUDIO_CHECK_SHA256));
        Map<String, JsonNode> folders = new HashMap<>();
        for (Map.Entry<String, JsonNode> e : everyAyah.properties()) {
            if (e.getValue().has("subfolder")) {
                folders.put(e.getValue().get("subfolder").asString(), e.getValue());
            }
        }
        Connection connection = context.getConnection();
        int next = maxRecitationId(connection) + 1;
        List<V1_3_1__ImportRecitations.Recitation> added = new ArrayList<>();
        for (String[] row : rows) {
            check(Integer.parseInt(row[0]) == next++, "the added ids follow the last recitation (row " + row[0] + ")");
            check(row[8].isBlank(), "an added EveryAyah recitation has no fallback (row " + row[0] + ")");
            V1_3_1__ImportRecitations.Recitation r = V1_3_1__ImportRecitations.fromEveryAyah(row, folders, audio);
            r.reciterId = reciterId(connection, Integer.parseInt(row[1]));
            added.add(r);
        }
        V1_3_1__ImportRecitations.insertRecitations(connection, added, new int[0], Map.of(), Set.of());
        try (PreparedStatement ps = connection.prepareStatement("insert into content_source (code, name, "
                + "resource_path, sha256, licence, source_url, obtained_on) values (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, "RECITATIONS_ADDED");
            ps.setString(2, "Recitations added after the first import (tajweed-backend)");
            ps.setString(3, ADDED);
            ps.setString(4, ADDED_SHA256);
            ps.setString(5, "MIT (this project)");
            ps.setString(6, "https://github.com/Omar-Sherif-Sayed/tajweed-backend");
            ps.setDate(7, Date.valueOf(OBTAINED));
            ps.executeUpdate();
        }
    }

    /**
     * The highest recitation id imported so far.
     *
     * @param connection the migration's connection
     * @return the id
     * @throws SQLException if it cannot be read
     */
    private static int maxRecitationId(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("select max(pk_id) from recitation");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /**
     * Our id of a reciter already in the catalogue.
     *
     * @param connection the migration's connection
     * @param mp3quranId mp3quran's id of the reciter
     * @return our id
     * @throws SQLException if it cannot be read
     */
    private static int reciterId(Connection connection, int mp3quranId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("select pk_id from reciter where mp3quran_id = ?")) {
            ps.setInt(1, mp3quranId);
            try (ResultSet rs = ps.executeQuery()) {
                check(rs.next(), "mp3quran reciter " + mp3quranId + " is already in the catalogue");
                return rs.getInt(1);
            }
        }
    }

    /**
     * Stops the migration when a condition does not hold.
     *
     * @param condition what must be true
     * @param what      what was being checked
     * @throws IllegalStateException if the condition is false
     */
    private static void check(boolean condition, String what) {
        if (!condition) {
            throw new IllegalStateException("Recitation addition stopped — check failed: " + what);
        }
    }
}
