package io.github.omarsherifsayed.tajweed.business.hadith.scheduler;

import io.github.omarsherifsayed.tajweed.business.hadith.client.HadeethEncClient;
import io.github.omarsherifsayed.tajweed.business.source.entity.ContentSync;
import io.github.omarsherifsayed.tajweed.business.source.repository.ContentSyncRepository;
import io.github.omarsherifsayed.tajweed.general.i18n.SupportedLanguages;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Fetches HadeethEnc's content into the database and keeps it fresh — the author's decision of 2026-11-22: their
 * content lives in the database only, never in a repository.
 *
 * <ol>
 *   <li>the categories, with their titles in every language of ours that HadeethEnc publishes;</li>
 *   <li>the list of hadiths, from the root categories (a root lists its sub-categories' hadiths too); when the list
 *       is much shorter than what is stored (under 90 %), the run stops — a broken answer never deletes content;
 *       otherwise hadiths no longer listed are removed;</li>
 *   <li>every hadith in every language of ours it is translated into — only when the stored text is missing or older
 *       than {@code refreshAfter}, so a stopped run continues where it stopped. An answer without its id, text, title
 *       or grade is refused and the stored text kept.</li>
 * </ol>
 * One request at a time, {@code delay} apart. Texts are stored exactly as received, with the SHA-256 of the answer.
 */
@Slf4j
@Component
public class HadeethEncSync {

    /** The code of this sync in {@code content_sync}. */
    public static final String CODE = "HADEETHENC";

    /** HadeethEnc's largest page. */
    private static final int PER_PAGE = 100;

    /** The share of the stored hadiths a new list must hold before the missing ones are removed. */
    private static final double MIN_LIST_SHARE = 0.9;

    /** HadeethEnc's API. */
    private final HadeethEncClient client;

    /** Writes the rows (one statement at a time, each its own transaction). */
    private final JdbcTemplate jdbc;

    /** Records the run. */
    private final ContentSyncRepository contentSyncRepository;

    /** The pause and the refresh age. */
    private final HadeethEncProperties properties;

    /** The time (a fixed clock in tests). */
    private final Clock clock;

    /** Reads the answers. */
    private final JsonMapper json = JsonMapper.builder().build();

    /**
     * Creates the sync.
     *
     * @param client                HadeethEnc's API
     * @param jdbc                  the database
     * @param contentSyncRepository the runs' state
     * @param properties            the settings
     * @param clock                 the time
     */
    public HadeethEncSync(HadeethEncClient client, JdbcTemplate jdbc, ContentSyncRepository contentSyncRepository,
                          HadeethEncProperties properties, Clock clock) {
        this.client = client;
        this.jdbc = jdbc;
        this.contentSyncRepository = contentSyncRepository;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Whether a sync is due: never finished, or the last full sync is older than {@code refreshAfter}.
     *
     * @return {@code true} when it should run
     */
    public boolean isDue() {
        return contentSyncRepository.findByCode(CODE)
                .map(s -> s.getLastSuccessAt() == null || !ContentSync.OK.equals(s.getStatus())
                        || s.getLastSuccessAt().isBefore(clock.instant().minus(properties.refreshAfter())))
                .orElse(true);
    }

    /**
     * Runs one sync.
     *
     * @return how many hadith texts were written
     */
    public int run() {
        ContentSync state = contentSyncRepository.findByCode(CODE).orElseGet(() -> new ContentSync(CODE));
        state.start(clock.instant());
        state = contentSyncRepository.save(state);
        int written = 0;
        try {
            Set<String> languages = languages();
            syncCategories(languages);
            Map<Integer, Set<String>> listed = listHadiths(languages);
            removeUnlisted(listed.keySet());
            Instant staleBefore = clock.instant().minus(properties.refreshAfter());
            for (Map.Entry<Integer, Set<String>> e : listed.entrySet()) {
                jdbc.update("insert into hadeethenc_hadith (pk_id, listed_at) values (?, ?) "
                        + "on conflict (pk_id) do update set listed_at = excluded.listed_at, updated_at = now()",
                        e.getKey(), Timestamp.from(clock.instant()));
                for (String language : e.getValue()) {
                    if (isFresh(e.getKey(), language, staleBefore)) {
                        continue;
                    }
                    if (fetchText(e.getKey(), language)) {
                        written++;
                    }
                }
            }
            state.succeed(written, null, clock.instant());
            log.info("HadeethEnc sync finished: {} hadiths listed, {} texts written", listed.size(), written);
        } catch (RuntimeException e) {
            state.fail(written, e.toString(), clock.instant());
            log.warn("HadeethEnc sync stopped after {} texts: {}", written, e.toString());
        }
        contentSyncRepository.save(state);
        return written;
    }

    /**
     * Our languages that HadeethEnc publishes, Arabic first.
     *
     * @return their codes
     */
    private Set<String> languages() {
        Set<String> theirs = new LinkedHashSet<>();
        for (JsonNode l : read(client.languages())) {
            theirs.add(l.get("code").asString());
        }
        Set<String> ours = new LinkedHashSet<>();
        for (String code : SupportedLanguages.CODES) {
            if (theirs.contains(code)) {
                ours.add(code);
            }
        }
        check(ours.contains("ar"), "HadeethEnc publishes Arabic");
        return ours;
    }

    /**
     * The categories (from the Arabic list) and their titles in every language.
     *
     * @param languages our languages that HadeethEnc publishes, Arabic first
     */
    private void syncCategories(Set<String> languages) {
        for (String language : languages) {
            JsonNode list;
            try {
                list = read(client.categories(language));
            } catch (RestClientException e) {
                log.debug("HadeethEnc categories in {}: {}", language, e.toString());
                continue;
            }
            for (JsonNode c : list) {
                int id = Integer.parseInt(c.get("id").asString());
                if ("ar".equals(language)) {
                    JsonNode parent = c.get("parent_id");
                    jdbc.update("insert into hadeethenc_category (pk_id, parent_id) values (?, ?) on conflict (pk_id) "
                            + "do update set parent_id = excluded.parent_id, updated_at = now()",
                            id, parent == null || parent.isNull() ? null : Integer.parseInt(parent.asString()));
                }
                String title = text(c, "title");
                if (title != null && exists("select count(*) from hadeethenc_category where pk_id = ?", id)) {
                    jdbc.update("insert into hadeethenc_category_title (category_id, language, title) values (?, ?, ?) "
                            + "on conflict (category_id, language) do update set title = excluded.title, "
                            + "updated_at = now()", id, language, title);
                }
            }
        }
    }

    /**
     * Every hadith of the root categories, with the languages of ours it is translated into.
     *
     * @param languages our languages that HadeethEnc publishes
     * @return hadith id → its languages, Arabic first
     */
    private Map<Integer, Set<String>> listHadiths(Set<String> languages) {
        List<Integer> roots = jdbc.queryForList("select pk_id from hadeethenc_category where parent_id is null "
                + "order by pk_id", Integer.class);
        check(!roots.isEmpty(), "HadeethEnc's root categories");
        Map<Integer, Set<String>> listed = new LinkedHashMap<>();
        for (int root : roots) {
            int page = 1;
            int last = 1;
            do {
                JsonNode answer = read(client.hadiths("ar", root, page, PER_PAGE));
                last = answer.get("meta").get("last_page").asInt();
                for (JsonNode h : answer.get("data")) {
                    Set<String> langs = new LinkedHashSet<>();
                    langs.add("ar");
                    for (JsonNode t : h.get("translations")) {
                        if (languages.contains(t.asString())) {
                            langs.add(t.asString());
                        }
                    }
                    listed.computeIfAbsent(Integer.parseInt(h.get("id").asString()), k -> new LinkedHashSet<>())
                            .addAll(langs);
                }
                page++;
            } while (page <= last);
        }
        return listed;
    }

    /**
     * Removes the hadiths HadeethEnc no longer lists — only when the new list is not much shorter than what is stored.
     *
     * @param listed the hadiths listed now
     */
    private void removeUnlisted(Set<Integer> listed) {
        Integer stored = jdbc.queryForObject("select count(*) from hadeethenc_hadith", Integer.class);
        check(!listed.isEmpty() && (stored == null || listed.size() >= stored * MIN_LIST_SHARE),
                "HadeethEnc's list holds " + listed.size() + " hadiths, fewer than 90 % of the " + stored + " stored");
        List<Integer> gone = new ArrayList<>();
        for (Integer id : jdbc.queryForList("select pk_id from hadeethenc_hadith", Integer.class)) {
            if (!listed.contains(id)) {
                gone.add(id);
            }
        }
        for (Integer id : gone) {
            jdbc.update("delete from hadeethenc_hadith where pk_id = ?", id);
        }
        if (!gone.isEmpty()) {
            log.info("HadeethEnc: {} hadiths no longer listed were removed", gone.size());
        }
    }

    /**
     * Fetches one hadith in one language and stores it, when the answer is whole.
     *
     * @param id       the hadith
     * @param language the language
     * @return {@code true} when a text was written
     */
    private boolean fetchText(int id, String language) {
        String body;
        try {
            body = client.hadith(language, id);
        } catch (RestClientException e) {
            log.debug("HadeethEnc hadith {} in {}: {}", id, language, e.toString());
            return false;
        }
        JsonNode h = read(body);
        String title = text(h, "title");
        String hadeeth = text(h, "hadeeth");
        String grade = text(h, "grade");
        if (h.get("id") == null || !String.valueOf(id).equals(h.get("id").asString()) || title == null
                || hadeeth == null || grade == null) {
            log.warn("HadeethEnc hadith {} in {} refused: incomplete answer", id, language);
            return false;
        }
        jdbc.update("insert into hadeethenc_hadith_text (hadith_id, language, title, hadeeth, hadeeth_intro, attribution, "
                + "grade, explanation, hints, words_meanings, reference, sha256, fetched_at) "
                + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) on conflict (hadith_id, language) do update set "
                + "title = excluded.title, hadeeth = excluded.hadeeth, hadeeth_intro = excluded.hadeeth_intro, "
                + "attribution = excluded.attribution, grade = excluded.grade, explanation = excluded.explanation, "
                + "hints = excluded.hints, words_meanings = excluded.words_meanings, reference = excluded.reference, "
                + "sha256 = excluded.sha256, fetched_at = excluded.fetched_at, updated_at = now()",
                id, language, title, hadeeth, text(h, "hadeeth_intro"), text(h, "attribution"), grade,
                text(h, "explanation"), array(h, "hints"), array(h, "words_meanings"), text(h, "reference"),
                sha256(body), Timestamp.from(clock.instant()));
        if ("ar".equals(language) && h.get("categories") != null) {
            jdbc.update("delete from hadeethenc_hadith_category where hadith_id = ?", id);
            for (JsonNode c : h.get("categories")) {
                int category = Integer.parseInt(c.asString());
                if (exists("select count(*) from hadeethenc_category where pk_id = ?", category)) {
                    jdbc.update("insert into hadeethenc_hadith_category (hadith_id, category_id) values (?, ?)",
                            id, category);
                }
            }
        }
        return true;
    }

    /**
     * Whether a stored text is newer than the refresh age.
     *
     * @param id          the hadith
     * @param language    the language
     * @param staleBefore texts fetched before this are fetched again
     * @return {@code true} when it need not be fetched
     */
    private boolean isFresh(int id, String language, Instant staleBefore) {
        return exists("select count(*) from hadeethenc_hadith_text where hadith_id = ? and language = ? "
                + "and fetched_at >= ?", id, language, Timestamp.from(staleBefore));
    }

    /**
     * Reads an answer, after the pause between requests.
     *
     * @param body the answer as received
     * @return its JSON
     */
    private JsonNode read(String body) {
        pause();
        check(body != null && !body.isBlank(), "a non-empty answer from HadeethEnc");
        return json.readTree(body);
    }

    /**
     * Waits {@code delay}, so that HadeethEnc receives one request at a time, gently.
     */
    private void pause() {
        if (properties.delay().isZero()) {
            return;
        }
        try {
            Thread.sleep(properties.delay());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("HadeethEnc sync interrupted", e);
        }
    }

    /**
     * Whether a counting query finds a row.
     *
     * @param sql  the query
     * @param args its arguments
     * @return {@code true} when the count is above zero
     */
    private boolean exists(String sql, Object... args) {
        Integer n = jdbc.queryForObject(sql, Integer.class, args);
        return n != null && n > 0;
    }

    /**
     * A text field as received, or {@code null} when it is missing or blank.
     *
     * @param node  the object
     * @param field the field
     * @return its value
     */
    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() || v.asString().isBlank() ? null : v.asString();
    }

    /**
     * An array field as the JSON text received, or {@code null}.
     *
     * @param node  the object
     * @param field the field
     * @return the array's JSON
     */
    private static String array(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || !v.isArray() ? null : v.toString();
    }

    /**
     * The SHA-256 of an answer.
     *
     * @param body the answer as received
     * @return 64 hexadecimal digits
     */
    private static String sha256(String body) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(body.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /**
     * Stops the run when a condition does not hold.
     *
     * @param condition what must be true
     * @param what      what was checked
     */
    private static void check(boolean condition, String what) {
        if (!condition) {
            throw new IllegalStateException("HadeethEnc sync stopped — check failed: " + what);
        }
    }
}
