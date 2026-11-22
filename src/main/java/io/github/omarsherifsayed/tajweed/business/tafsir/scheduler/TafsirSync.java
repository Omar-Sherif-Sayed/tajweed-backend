package io.github.omarsherifsayed.tajweed.business.tafsir.scheduler;

import io.github.omarsherifsayed.tajweed.business.tafsir.client.QuranFoundationClient;
import io.github.omarsherifsayed.tajweed.business.tafsir.client.QuranFoundationException;
import io.github.omarsherifsayed.tajweed.business.tafsir.client.QuranFoundationProperties;
import io.github.omarsherifsayed.tajweed.business.source.entity.ContentSync;
import io.github.omarsherifsayed.tajweed.business.source.repository.ContentSyncRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;

/**
 * Keeps the chosen tafsirs ({@code content/reviewed/tafsirs.csv}) in the database through the Quran Foundation's
 * Content Sync, as their guide describes:
 *
 * <ol>
 *   <li>the tafsirs' names (in English and Arabic) from {@code /api/v4/resources/tafsirs};</li>
 *   <li>{@code /api/v4/resources/sync} with {@code bootstrap=true} the first time, then with the stored
 *       {@code sync_token}, following {@code next_page_url} while {@code has_more}; the token of the last page is kept;</li>
 *   <li>each change applied: a whole tafsir replaced from its snapshot ({@code RESOURCE_CREATE},
 *       {@code RESOURCE_INVALIDATE}), a row added, changed or removed, a tafsir removed; a snapshot without records is
 *       refused and the stored text kept;</li>
 *   <li>when the API asks for it ({@code resync_required}, a changed filter), the token is forgotten and the sync
 *       starts again from a bootstrap.</li>
 * </ol>
 * A row covers one ayah or a range ({@code group_verse_key_from} … {@code group_verse_key_to}); texts are stored as
 * received.
 */
@Slf4j
@Component
public class TafsirSync {

    /** The code of this sync in {@code content_sync}. */
    public static final String CODE = "QF_TAFSIRS";

    /** The chosen tafsirs. */
    static final String CHOICE = "content/reviewed/tafsirs.csv";

    /** The API's error types after which the sync starts again from a bootstrap. */
    private static final List<String> RESTART = List.of("resync_required", "token_filter_mismatch",
            "cursor_filter_mismatch");

    /** The Quran Foundation's API. */
    private final QuranFoundationClient client;

    /** The settings. */
    private final QuranFoundationProperties properties;

    /** Writes the rows. */
    private final JdbcTemplate jdbc;

    /** Replaces a tafsir's rows in one transaction. */
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
    public TafsirSync(QuranFoundationClient client, QuranFoundationProperties properties, JdbcTemplate jdbc,
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
     * @return how many tafsir rows were written
     */
    public int run() {
        if (!properties.hasCredentials()) {
            log.info("Tafsir sync off: QF_CLIENT_ID and QF_CLIENT_SECRET are not set");
            return 0;
        }
        ContentSync state = contentSyncRepository.findByCode(CODE).orElseGet(() -> new ContentSync(CODE));
        state.start(clock.instant());
        state = contentSyncRepository.save(state);
        int[] written = {0};
        try {
            Map<Integer, Integer> chosen = chosen();
            syncNames(chosen);
            String token;
            try {
                token = syncChanges(chosen, state.getSyncToken(), written);
            } catch (QuranFoundationException e) {
                if (!RESTART.contains(e.getType())) {
                    throw e;
                }
                log.info("Content Sync asked to start again ({}): bootstrap", e.getType());
                token = syncChanges(chosen, null, written);
            }
            state.succeed(written[0], token, clock.instant());
            log.info("Tafsir sync finished: {} rows written", written[0]);
        } catch (RuntimeException e) {
            state.fail(written[0], e.getMessage(), clock.instant());
            log.warn("Tafsir sync stopped after {} rows: {}", written[0], e.getMessage());
        }
        contentSyncRepository.save(state);
        return written[0];
    }

    /**
     * The tafsirs' names, for the chosen ones, from the list in English and in Arabic.
     *
     * @param chosen resource id → place in the list
     */
    private void syncNames(Map<Integer, Integer> chosen) {
        Map<Integer, JsonNode> english = byId(client.get("/api/v4/resources/tafsirs?language=en").path("tafsirs"));
        Map<Integer, JsonNode> arabic = byId(client.get("/api/v4/resources/tafsirs?language=ar").path("tafsirs"));
        for (Map.Entry<Integer, Integer> c : chosen.entrySet()) {
            JsonNode en = english.get(c.getKey());
            check(en != null, "tafsir " + c.getKey() + " in the Quran Foundation's list");
            JsonNode ar = arabic.get(c.getKey());
            jdbc.update("insert into tafsir (pk_id, provider, name_ar, name_en, author_ar, author_en, language, sort_order) "
                    + "values (?, 'QURAN_FOUNDATION', ?, ?, ?, ?, ?, ?) on conflict (pk_id) do update set "
                    + "name_ar = excluded.name_ar, name_en = excluded.name_en, author_ar = excluded.author_ar, "
                    + "author_en = excluded.author_en, language = excluded.language, sort_order = excluded.sort_order, "
                    + "updated_at = now()",
                    c.getKey(), ar == null ? null : ar.path("translated_name").path("name").asString(null),
                    en.path("name").asString(), ar == null ? null : ar.path("author_name").asString(null),
                    en.path("author_name").asString(null), en.path("language_name").asString(""), c.getValue());
        }
    }

    /**
     * Follows the sync from a token (or a bootstrap) to its end and applies every change.
     *
     * @param chosen  the chosen tafsirs
     * @param token   the stored token, or {@code null} for a bootstrap
     * @param written counts the rows written
     * @return the token to store
     */
    private String syncChanges(Map<Integer, Integer> chosen, String token, int[] written) {
        StringBuilder filter = new StringBuilder();
        for (Integer id : chosen.keySet()) {
            filter.append(filter.isEmpty() ? "" : "%3B").append("tafsirs:").append(id);
        }
        String next = "/api/v4/resources/sync?per_page=100&resources=" + filter
                + (token == null ? "&bootstrap=true" : "&sync_token=" + token);
        String last = token;
        Map<String, Integer> ayahs = ayahIds();
        while (next != null) {
            JsonNode sync = client.get(next).path("sync");
            for (JsonNode m : sync.path("mutations")) {
                apply(m, chosen, ayahs, written);
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
     * @param chosen  the chosen tafsirs
     * @param ayahs   our ayah ids by verse key
     * @param written counts the rows written
     */
    private void apply(JsonNode m, Map<Integer, Integer> chosen, Map<String, Integer> ayahs, int[] written) {
        if (!"tafsirs".equals(m.path("resource_group").asString())) {
            return;
        }
        int resource = m.path("resource_id").asInt();
        if (!chosen.containsKey(resource)) {
            return;
        }
        String type = m.path("type").asString();
        switch (type) {
            case "RESOURCE_CREATE", "RESOURCE_INVALIDATE" -> {
                String url = m.path("snapshot_url").asString(null);
                if (url == null) {
                    log.warn("tafsir {}: {} without a snapshot ({})", resource, type,
                            m.path("unavailable_reason").asString(""));
                    return;
                }
                written[0] += replace(resource, client.get(url), ayahs);
            }
            case "RESOURCE_DELETE" -> jdbc.update("delete from tafsir_text where tafsir_id = ?", resource);
            case "ROW_CREATE", "ROW_UPDATE" -> {
                upsert(resource, m.path("data"), ayahs);
                written[0]++;
            }
            case "ROW_DELETE" -> jdbc.update("delete from tafsir_text where tafsir_id = ? and source_record_id = ?",
                    resource, Integer.parseInt(m.path("record_key").asString()));
            default -> {
                // RESOURCE_UPDATE: a freshness marker only
            }
        }
    }

    /**
     * Replaces a tafsir's rows with its snapshot, in one transaction — or keeps them when the snapshot is empty.
     *
     * @param resource the tafsir
     * @param snapshot the snapshot
     * @param ayahs    our ayah ids by verse key
     * @return the rows written
     */
    private int replace(int resource, JsonNode snapshot, Map<String, Integer> ayahs) {
        JsonNode records = snapshot.path("records");
        check(snapshot.path("resource_id").asInt() == resource, "the snapshot of tafsir " + resource);
        check(records.isArray() && !records.isEmpty(), "records in the snapshot of tafsir " + resource);
        Integer n = transaction.execute(status -> {
            jdbc.update("delete from tafsir_text where tafsir_id = ?", resource);
            int count = 0;
            for (JsonNode r : records) {
                upsert(resource, r, ayahs);
                count++;
            }
            return count;
        });
        return n == null ? 0 : n;
    }

    /**
     * Stores one row as received.
     *
     * @param resource the tafsir
     * @param row      the row: {@code id}, {@code verse_key}, {@code group_verse_key_from}, {@code group_verse_key_to},
     *                 {@code text}
     * @param ayahs    our ayah ids by verse key
     */
    private void upsert(int resource, JsonNode row, Map<String, Integer> ayahs) {
        String from = row.path("group_verse_key_from").asString(row.path("verse_key").asString(null));
        String to = row.path("group_verse_key_to").asString(from);
        Integer ayahFrom = ayahs.get(from);
        Integer ayahTo = ayahs.get(to);
        check(ayahFrom != null && ayahTo != null, "the ayahs " + from + " – " + to + " of a row of tafsir " + resource);
        jdbc.update("insert into tafsir_text (tafsir_id, source_record_id, ayah_id, ayah_to_id, text, synced_at) "
                + "values (?, ?, ?, ?, ?, ?) on conflict (tafsir_id, source_record_id) do update set "
                + "ayah_id = excluded.ayah_id, ayah_to_id = excluded.ayah_to_id, text = excluded.text, "
                + "synced_at = excluded.synced_at, updated_at = now()",
                resource, row.path("id").asInt(), ayahFrom, ayahTo, row.path("text").asString(""),
                Timestamp.from(clock.instant()));
    }

    /**
     * Our ayah ids, by the verse key {@code surah:ayah}.
     *
     * @return verse key → ayah id
     */
    private Map<String, Integer> ayahIds() {
        Map<String, Integer> map = new HashMap<>();
        jdbc.query("select surah_id, number_in_surah, pk_id from ayah", rs -> {
            map.put(rs.getInt(1) + ":" + rs.getInt(2), rs.getInt(3));
        });
        return map;
    }

    /**
     * The chosen tafsirs, from {@value #CHOICE}.
     *
     * @return resource id → place in the list, in the file's order
     */
    static Map<Integer, Integer> chosen() {
        Map<Integer, Integer> map = new LinkedHashMap<>();
        try (InputStream in = TafsirSync.class.getClassLoader().getResourceAsStream(CHOICE)) {
            check(in != null, "the file " + CHOICE);
            List<String> lines = new ArrayList<>(List.of(new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .split("\n")));
            boolean header = true;
            for (String line : lines) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                if (header) {
                    header = false;
                    continue;
                }
                String[] p = line.split(",", 3);
                map.put(Integer.parseInt(p[0].strip()), Integer.parseInt(p[1].strip()));
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + CHOICE, e);
        }
        check(!map.isEmpty(), "at least one tafsir chosen");
        return map;
    }

    /**
     * The entries of a JSON array by their {@code id}.
     *
     * @param array the array
     * @return id → entry
     */
    private static Map<Integer, JsonNode> byId(JsonNode array) {
        Map<Integer, JsonNode> map = new HashMap<>();
        for (JsonNode n : array) {
            map.put(n.path("id").asInt(), n);
        }
        return map;
    }

    /**
     * Stops the run when a condition does not hold.
     *
     * @param condition what must be true
     * @param what      what was checked
     */
    private static void check(boolean condition, String what) {
        if (!condition) {
            throw new IllegalStateException("Tafsir sync stopped — check failed: " + what);
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
