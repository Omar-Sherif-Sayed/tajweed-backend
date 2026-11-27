package io.github.omarsherifsayed.tajweed.business.quran;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.omarsherifsayed.tajweed.TestcontainersConfiguration;
import io.github.omarsherifsayed.tajweed.business.quran.scheduler.QfMushafSync;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Proves the mushaf sync and the page's glyphs against a small local server that answers like the Quran Foundation's
 * Content Sync for {@code mushafs:2} — with a snapshot built from our own {@code mushaf_line} and the placeholder glyph
 * «T», never their content. It proves: a bootstrap stores every word after the checks; {@code /v1/pages/{p}} gives the
 * glyphs line by line with the basmala and the credit; the next run applies a row change from the stored token; a
 * snapshot that misses a page is refused and the stored glyphs are kept.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class QfMushafSyncIntegrationTest {

    /** The local server. */
    private static final HttpServer SERVER = start();

    /** The database, for the server to build its snapshot from our lines. */
    private static JdbcTemplate database;

    /** Runs the sync. */
    @Autowired
    private QfMushafSync sync;

    /** Reads the database directly. */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Sends requests to the application without a server. */
    @Autowired
    private MockMvc mockMvc;

    /**
     * Points the client at the local server with test credentials.
     *
     * @param registry the test's properties
     */
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        String base = "http://127.0.0.1:" + SERVER.getAddress().getPort();
        registry.add("tajweed.quran-foundation.client-id", () -> "test-client");
        registry.add("tajweed.quran-foundation.client-secret", () -> "test-secret");
        registry.add("tajweed.quran-foundation.auth-url", () -> base + "/oauth2/token");
        registry.add("tajweed.quran-foundation.api-url", () -> base + "/content");
    }

    /**
     * Stops the local server.
     */
    @AfterAll
    static void stop() {
        SERVER.stop(0);
    }

    @Test
    @DisplayName("Bootstrap with checks, glyphs on the page, a row change, then a broken snapshot refused")
    void syncsChecksAndServes() throws Exception {
        database = jdbcTemplate;
        assertThat(sync.isDue()).isTrue();

        int written = sync.run();

        int words = count("select count(*) from qf_mushaf_word");
        assertThat(written).isEqualTo(words).isGreaterThan(6236);
        assertThat(token()).isEqualTo("token-1");
        mockMvc.perform(get("/v1/pages/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.qcf.lines.length()").value(15))
                .andExpect(jsonPath("$.data.qcf.lines[0].line").value(1))
                .andExpect(jsonPath("$.data.qcf.lines[0].glyphs[0].glyph").value("T"))
                .andExpect(jsonPath("$.data.qcf.basmala").value("T T T T"))
                .andExpect(jsonPath("$.data.qcf.credit").value("Quran fonts provided by Quran Foundation"));
        mockMvc.perform(get("/v1/pages/1"))
                .andExpect(jsonPath("$.data.qcf.lines.length()").value(7));

        sync.run();

        assertThat(token()).isEqualTo("token-2");
        assertThat(jdbcTemplate.queryForObject("select glyph from qf_mushaf_word where qf_word_id = 1", String.class))
                .isEqualTo("U");

        sync.run();

        assertThat(count("select count(*) from qf_mushaf_word")).isEqualTo(words);
        assertThat(jdbcTemplate.queryForObject("select status from content_sync where code = ?", String.class,
                QfMushafSync.CODE)).isEqualTo("FAILED");
    }

    /**
     * The stored sync token.
     *
     * @return the token
     */
    private String token() {
        return jdbcTemplate.queryForObject("select sync_token from content_sync where code = ?", String.class,
                QfMushafSync.CODE);
    }

    /**
     * Runs a counting query.
     *
     * @param sql a query returning one number
     * @return the number
     */
    private int count(String sql) {
        Integer n = jdbcTemplate.queryForObject(sql, Integer.class);
        return n == null ? 0 : n;
    }

    /**
     * Starts the local server.
     *
     * @return the server
     */
    private static HttpServer start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/oauth2/token", e -> write(e, 200,
                    "{\"access_token\":\"test-token\",\"token_type\":\"bearer\",\"expires_in\":3600}"));
            server.createContext("/content", QfMushafSyncIntegrationTest::content);
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Answers the Content API requests.
     *
     * @param e the request
     * @throws IOException if the answer cannot be written
     */
    private static void content(HttpExchange e) throws IOException {
        String path = e.getRequestURI().getPath();
        String query = e.getRequestURI().getRawQuery() == null ? "" : e.getRequestURI().getRawQuery();
        if (!"test-token".equals(e.getRequestHeaders().getFirst("x-auth-token"))) {
            write(e, 401, "{\"type\":\"unauthorized\"}");
        } else if (path.endsWith("/resources/sync") && query.contains("bootstrap=true")
                && query.contains("resources=mushafs%3A2")) {
            write(e, 200, "{\"sync\":{\"has_more\":false,\"next_sync_token\":\"token-1\",\"mutations\":[{\"sequence\":1,"
                    + "\"type\":\"RESOURCE_CREATE\",\"resource_group\":\"mushafs\",\"resource_id\":2,"
                    + "\"snapshot_url\":\"/api/v4/resources/snapshots/mushafs/2\",\"data\":null}]}}");
        } else if (path.endsWith("/resources/sync") && query.contains("sync_token=token-1")) {
            write(e, 200, "{\"sync\":{\"has_more\":false,\"next_sync_token\":\"token-2\",\"mutations\":[{\"sequence\":2,"
                    + "\"type\":\"ROW_UPDATE\",\"resource_group\":\"mushafs\",\"resource_id\":2,\"record_key\":\"1\","
                    + "\"data\":{\"id\":1,\"mushaf_id\":2,\"verse_id\":1,\"text\":\"U\",\"char_type_name\":\"word\","
                    + "\"page_number\":1,\"line_number\":2,\"position_in_page\":1,\"record_type\":\"mushaf_word\"}}]}}");
        } else if (path.endsWith("/resources/sync") && query.contains("sync_token=token-2")) {
            write(e, 200, "{\"sync\":{\"has_more\":false,\"next_sync_token\":\"token-3\",\"mutations\":[{\"sequence\":3,"
                    + "\"type\":\"RESOURCE_INVALIDATE\",\"resource_group\":\"mushafs\",\"resource_id\":2,"
                    + "\"snapshot_url\":\"/api/v4/resources/snapshots/mushafs/2?broken=1\",\"data\":null}]}}");
        } else if (path.endsWith("/resources/snapshots/mushafs/2")) {
            write(e, 200, snapshot(query.contains("broken=1")));
        } else {
            write(e, 404, "{\"type\":\"not_found\"}");
        }
    }

    /**
     * A snapshot built from our printed lines: one placeholder word per ayah on every text line it touches, and each
     * ayah's end marker on the last line it touches.
     *
     * @param broken leave page 604 out
     * @return the JSON
     */
    private static String snapshot(boolean broken) {
        List<Map<String, Object>> lines = database.queryForList("select page, line, first_ayah_id, last_ayah_id "
                + "from mushaf_line where kind = 'TEXT' order by page, line");
        Map<Integer, int[]> lastLine = new HashMap<>();
        for (Map<String, Object> l : lines) {
            for (int a = ((Number) l.get("first_ayah_id")).intValue(); a <= ((Number) l.get("last_ayah_id")).intValue();
                    a++) {
                lastLine.put(a, new int[] {((Number) l.get("page")).intValue(), ((Number) l.get("line")).intValue()});
            }
        }
        StringBuilder json = new StringBuilder("{\"resource_group\":\"mushafs\",\"resource_id\":2,\"schema_version\":1,"
                + "\"records\":[{\"id\":2,\"name\":\"QCF V1\",\"record_type\":\"mushaf\"}");
        int id = 0;
        int position = 0;
        int lastPage = 0;
        for (Map<String, Object> l : lines) {
            int page = ((Number) l.get("page")).intValue();
            int line = ((Number) l.get("line")).intValue();
            if (broken && page == 604) {
                continue;
            }
            position = page == lastPage ? position : 0;
            lastPage = page;
            for (int a = ((Number) l.get("first_ayah_id")).intValue(); a <= ((Number) l.get("last_ayah_id")).intValue();
                    a++) {
                int words = page == 1 && a == 1 ? 4 : 1;
                for (int w = 0; w < words; w++) {
                    json.append(record(++id, a, "word", page, line, ++position));
                }
                int[] end = lastLine.get(a);
                if (end[0] == page && end[1] == line) {
                    json.append(record(++id, a, "end", page, line, ++position));
                }
            }
        }
        return json.append("]}").toString();
    }

    /**
     * One {@code mushaf_word} record with the placeholder glyph.
     *
     * @param id       its id
     * @param ayah     its ayah
     * @param type     {@code word} or {@code end}
     * @param page     its page
     * @param line     its line
     * @param position its place on the page
     * @return the JSON, with a leading comma
     */
    private static String record(int id, int ayah, String type, int page, int line, int position) {
        return ",{\"id\":" + id + ",\"mushaf_id\":2,\"word_id\":" + id + ",\"verse_id\":" + ayah
                + ",\"text\":\"T\",\"char_type_name\":\"" + type + "\",\"page_number\":" + page + ",\"line_number\":"
                + line + ",\"position_in_page\":" + position + ",\"record_type\":\"mushaf_word\"}";
    }

    /**
     * Writes a JSON answer.
     *
     * @param e      the request
     * @param status the status
     * @param body   the JSON
     * @throws IOException if it cannot be written
     */
    private static void write(HttpExchange e, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().add("Content-Type", "application/json");
        e.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = e.getResponseBody()) {
            out.write(bytes);
        }
        e.close();
    }
}
