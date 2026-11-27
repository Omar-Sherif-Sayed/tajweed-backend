package io.github.omarsherifsayed.tajweed.business.quran.scheduler;

import io.github.omarsherifsayed.tajweed.business.source.entity.ContentSync;
import io.github.omarsherifsayed.tajweed.business.source.repository.ContentSyncRepository;
import io.github.omarsherifsayed.tajweed.business.tafsir.client.QuranFoundationClient;
import io.github.omarsherifsayed.tajweed.business.tafsir.client.QuranFoundationException;
import io.github.omarsherifsayed.tajweed.business.tafsir.client.QuranFoundationProperties;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;

/**
 * Keeps the glyphs of the Quran Foundation's mushaf 2 — QCF V1, the King Fahd Complex's 1405H print — in
 * {@code qf_mushaf_word} through their Content Sync (resource {@code mushafs:2}), as the tafsir sync does: a bootstrap
 * the first time, then the stored {@code sync_token}; a snapshot replaces every row, a row change is applied as it
 * comes, and the sync starts again from a bootstrap when the API asks for it.
 *
 * <p>A snapshot is stored only when it agrees with the printed lines we already hold ({@code mushaf_line}, the
 * DigitalKhatt layout checked against the Tanzil text): every page 1 – 604 present, every ayah 1 – 6236 with exactly one
 * end marker, and every line holding the same ayahs as our line — the one difference allowed being an ayah's number
 * at the start of the next line (566 lines, as the King Fahd print sets them; found on the first real snapshot,
 * 2026-11-27). Otherwise the rows already stored are kept and the run is recorded as failed.
 */
@Slf4j
@Component
public class QfMushafSync {

    /** The code of this sync in {@code content_sync}. */
    public static final String CODE = "QF_MUSHAF_QCF_V1";

    /** The Quran Foundation's id of the mushaf: QCF V1, «King Fahd Complex Mushaf 1405 hijri print». */
    public static final short MUSHAF_ID = 2;

    /** The API's error types after which the sync starts again from a bootstrap. */
    private static final List<String> RESTART = List.of("resync_required", "token_filter_mismatch",
            "cursor_filter_mismatch");

    /** The SQL that stores one glyph. */
    private static final String UPSERT = "insert into qf_mushaf_word (mushaf_id, qf_word_id, ayah_id, position, "
            + "char_type, page, line, glyph, synced_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?) "
            + "on conflict (mushaf_id, qf_word_id) do update set ayah_id = excluded.ayah_id, "
            + "position = excluded.position, char_type = excluded.char_type, page = excluded.page, "
            + "line = excluded.line, glyph = excluded.glyph, synced_at = excluded.synced_at, updated_at = now()";

    /** The Quran Foundation's API. */
    private final QuranFoundationClient client;

    /** The settings. */
    private final QuranFoundationProperties properties;

    /** Writes the rows. */
    private final JdbcTemplate jdbc;

    /** Replaces the rows in one transaction. */
    private final TransactionTemplate transaction;

    /** Records the runs. */
    private final ContentSyncRepository contentSyncRepository;

    /** The time. */
    private final Clock clock;

    /**
     * Creates the sync.
     *
     * @param client                the Quran Foundation's API
     * @param properties            the settings
     * @param jdbc                  the database
     * @param transaction           transactions
     * @param contentSyncRepository the runs' state
     * @param clock                 the time
     */
    public QfMushafSync(QuranFoundationClient client, QuranFoundationProperties properties, JdbcTemplate jdbc,
                        TransactionTemplate transaction, ContentSyncRepository contentSyncRepository, Clock clock) {
        this.client = client;
        this.properties = properties;
        this.jdbc = jdbc;
        this.transaction = transaction;
        this.contentSyncRepository = contentSyncRepository;
        this.clock = clock;
    }

    /**
     * Whether a sync is due: the credentials are set, and the last sync never finished or is older than
     * {@code refreshAfter}.
     *
     * @return {@code true} when it should run
     */
    public boolean isDue() {
        if (!properties.hasCredentials()) {
            return false;
        }
        return contentSyncRepository.findByCode(CODE)
                .map(s -> s.getLastSuccessAt() == null || !ContentSync.OK.equals(s.getStatus())
                        || s.getLastSuccessAt().isBefore(clock.instant().minus(properties.refreshAfter())))
                .orElse(true);
    }

    /**
     * Runs one sync.
     *
     * @return how many glyph rows were written
     */
    public int run() {
        if (!properties.hasCredentials()) {
            log.info("Mushaf sync off: QF_CLIENT_ID and QF_CLIENT_SECRET are not set");
            return 0;
        }
        ContentSync state = contentSyncRepository.findByCode(CODE).orElseGet(() -> new ContentSync(CODE));
        state.start(clock.instant());
        state = contentSyncRepository.save(state);
        int[] written = {0};
        try {
            String token;
            try {
                token = syncChanges(state.getSyncToken(), written);
            } catch (QuranFoundationException e) {
                if (!RESTART.contains(e.getType())) {
                    throw e;
                }
                log.info("Content Sync asked to start again ({}): bootstrap", e.getType());
                token = syncChanges(null, written);
            }
            state.succeed(written[0], token, clock.instant());
            log.info("Mushaf sync finished: {} glyphs written", written[0]);
        } catch (RuntimeException e) {
            state.fail(written[0], e.getMessage(), clock.instant());
            log.warn("Mushaf sync stopped after {} glyphs: {}", written[0], e.getMessage());
        }
        contentSyncRepository.save(state);
        return written[0];
    }

    /**
     * Follows the sync from a token (or a bootstrap) to its end and applies every change.
     *
     * @param token   the stored token, or {@code null} for a bootstrap
     * @param written counts the rows written
     * @return the token to store
     */
    private String syncChanges(String token, int[] written) {
        String next = "/api/v4/resources/sync?per_page=100&resources=mushafs%3A" + MUSHAF_ID
                + (token == null ? "&bootstrap=true" : "&sync_token=" + token);
        String last = token;
        while (next != null) {
            JsonNode sync = client.get(next).path("sync");
            for (JsonNode m : sync.path("mutations")) {
                apply(m, written);
            }
            if (sync.path("has_more").asBoolean(false)) {
                String url = sync.path("next_page_url").asString(null);
                check(url != null, "next_page_url when has_more");
                next = url;
            } else {
                last = sync.path("next_sync_token").asString(last);
                next = null;
            }
        }
        return last;
    }

    /**
     * Applies one change.
     *
     * @param m       the mutation
     * @param written counts the rows written
     */
    private void apply(JsonNode m, int[] written) {
        if (!"mushafs".equals(m.path("resource_group").asString()) || m.path("resource_id").asInt() != MUSHAF_ID) {
            return;
        }
        String type = m.path("type").asString();
        switch (type) {
            case "RESOURCE_CREATE", "RESOURCE_INVALIDATE" -> {
                String url = m.path("snapshot_url").asString(null);
                if (url == null) {
                    log.warn("mushaf {}: {} without a snapshot ({})", MUSHAF_ID, type,
                            m.path("unavailable_reason").asString(""));
                    return;
                }
                written[0] += replace(client.get(url));
            }
            case "RESOURCE_DELETE" -> jdbc.update("delete from qf_mushaf_word where mushaf_id = ?", MUSHAF_ID);
            case "ROW_CREATE", "ROW_UPDATE" -> {
                JsonNode data = m.path("data");
                if ("mushaf_word".equals(data.path("record_type").asString())) {
                    store(List.of(data));
                    written[0]++;
                }
            }
            case "ROW_DELETE" -> jdbc.update("delete from qf_mushaf_word where mushaf_id = ? and qf_word_id = ?",
                    MUSHAF_ID, Integer.parseInt(m.path("record_key").asString()));
            default -> {
                // RESOURCE_UPDATE: a freshness marker only
            }
        }
    }

    /**
     * Replaces every glyph with the snapshot's, in one transaction — after the checks of this class.
     *
     * @param snapshot the snapshot
     * @return the rows written
     */
    private int replace(JsonNode snapshot) {
        check(snapshot.path("resource_id").asInt() == MUSHAF_ID, "the snapshot of mushaf " + MUSHAF_ID);
        List<JsonNode> words = new ArrayList<>();
        for (JsonNode r : snapshot.path("records")) {
            if ("mushaf_word".equals(r.path("record_type").asString())) {
                words.add(r);
            }
        }
        verify(words);
        Integer n = transaction.execute(status -> {
            jdbc.update("delete from qf_mushaf_word where mushaf_id = ?", MUSHAF_ID);
            store(words);
            return words.size();
        });
        return n == null ? 0 : n;
    }

    /**
     * Stores glyph rows as received.
     *
     * @param words the {@code mushaf_word} records
     */
    private void store(List<JsonNode> words) {
        Timestamp now = Timestamp.from(clock.instant());
        List<Object[]> rows = new ArrayList<>();
        for (JsonNode w : words) {
            String charType = w.path("char_type_name").asString();
            check("word".equals(charType) || "end".equals(charType), "a word's type: " + charType);
            rows.add(new Object[] {MUSHAF_ID, w.path("id").asInt(), (short) w.path("verse_id").asInt(),
                (short) w.path("position_in_page").asInt(), charType, (short) w.path("page_number").asInt(),
                (short) w.path("line_number").asInt(), w.path("text").asString(), now});
        }
        jdbc.batchUpdate(UPSERT, rows);
    }

    /**
     * Checks a snapshot's words against our printed lines.
     *
     * @param words the {@code mushaf_word} records
     */
    private void verify(List<JsonNode> words) {
        check(!words.isEmpty(), "words in the snapshot");
        Map<String, List<JsonNode>> byLine = new TreeMap<>();
        Set<Integer> pages = new HashSet<>();
        Map<Integer, Integer> ends = new HashMap<>();
        for (JsonNode w : words) {
            int page = w.path("page_number").asInt();
            int line = w.path("line_number").asInt();
            int ayah = w.path("verse_id").asInt();
            check(page >= 1 && page <= 604 && line >= 1 && line <= 15, "the place of word " + w.path("id").asInt());
            check(ayah >= 1 && ayah <= 6236, "the ayah of word " + w.path("id").asInt());
            check(!w.path("text").asString("").isEmpty(), "the glyph of word " + w.path("id").asInt());
            pages.add(page);
            if ("end".equals(w.path("char_type_name").asString())) {
                ends.merge(ayah, 1, Integer::sum);
            }
            byLine.computeIfAbsent(page + ":" + line, k -> new ArrayList<>()).add(w);
        }
        check(pages.size() == 604, "604 pages, not " + pages.size());
        check(ends.size() == 6236 && ends.values().stream().allMatch(c -> c == 1), "one end marker for every ayah");
        Map<String, int[]> ours = new HashMap<>();
        jdbc.query("select page, line, kind, first_ayah_id, last_ayah_id from mushaf_line", rs -> {
            ours.put(rs.getInt(1) + ":" + rs.getInt(2), "TEXT".equals(rs.getString(3))
                    ? new int[] {rs.getInt(4), rs.getInt(5)} : null);
        });
        for (Map.Entry<String, List<JsonNode>> e : byLine.entrySet()) {
            int[] our = ours.get(e.getKey());
            check(our != null, "line " + e.getKey() + " is a text line of ours");
            List<JsonNode> line = new ArrayList<>(e.getValue());
            line.sort(Comparator.comparingInt(w -> w.path("position_in_page").asInt()));
            JsonNode first = line.get(0);
            if ("end".equals(first.path("char_type_name").asString()) && first.path("verse_id").asInt() == our[0] - 1) {
                line.remove(0);
            }
            int min = line.stream().mapToInt(w -> w.path("verse_id").asInt()).min().orElse(-1);
            int max = line.stream().mapToInt(w -> w.path("verse_id").asInt()).max().orElse(-1);
            check(min == our[0] && max == our[1], "the ayahs of line " + e.getKey() + ": " + min + " – " + max
                    + ", ours " + our[0] + " – " + our[1]);
        }
        long textLines = ours.values().stream().filter(v -> v != null).count();
        check(byLine.size() == textLines, "every text line of ours present: " + byLine.size() + " of " + textLines);
    }

    /**
     * Stops the run when a condition does not hold.
     *
     * @param condition what must be true
     * @param what      what was checked
     */
    private static void check(boolean condition, String what) {
        if (!condition) {
            throw new IllegalStateException("Mushaf sync stopped — check failed: " + what);
        }
    }

    /**
     * When the last sync succeeded, for the tests.
     *
     * @return the time, or {@code null}
     */
    Instant lastSuccess() {
        return contentSyncRepository.findByCode(CODE).map(ContentSync::getLastSuccessAt).orElse(null);
    }
}
