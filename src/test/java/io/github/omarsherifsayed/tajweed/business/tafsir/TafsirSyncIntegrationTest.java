package io.github.omarsherifsayed.tajweed.business.tafsir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.omarsherifsayed.tajweed.TestcontainersConfiguration;
import io.github.omarsherifsayed.tajweed.business.tafsir.scheduler.TafsirSync;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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
 * Proves the tafsir sync and endpoints against a small local server that answers like the Quran Foundation's token
 * service and Content Sync — with placeholder texts marked «TEST ONLY», never their content. It proves: the token is
 * asked with HTTP Basic and sent as {@code x-auth-token} with {@code x-client-id}; a bootstrap follows
 * {@code next_page_url}, replaces a tafsir from its snapshot and applies a row change; a text covering a range answers
 * for every ayah of the range; the next run continues from the stored {@code sync_token} and applies a deletion.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TafsirSyncIntegrationTest {

    /** The chosen tafsir ids, as {@code content/reviewed/tafsirs.csv} lists them. */
    private static final List<Integer> CHOSEN = List.of(14, 15, 90, 94, 91, 16, 93);

    /** Requests that came with the right token and client id. */
    private static final AtomicInteger AUTHORISED = new AtomicInteger();

    /** The local server. */
    private static final HttpServer SERVER = start();

    /** Runs the sync. */
    @Autowired
    private TafsirSync sync;

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
    @DisplayName("Bootstrap, snapshot, row change, range lookup; then the next run continues from the token")
    void syncsAndServes() throws Exception {
        assertThat(sync.isDue()).isTrue();
        int written = sync.run();

        assertThat(written).isEqualTo(3);
        assertThat(AUTHORISED.get()).isGreaterThanOrEqualTo(5);
        assertThat(count("select count(*) from tafsir")).isEqualTo(7);
        assertThat(jdbcTemplate.queryForObject("select sync_token from content_sync where code = 'QF_TAFSIRS'",
                String.class)).isEqualTo("token-1");
        mockMvc.perform(get("/v1/surahs/1/ayahs/3/tafsirs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tafsirs.length()").value(1))
                .andExpect(jsonPath("$.data.tafsirs[0].tafsir.id").value(14))
                .andExpect(jsonPath("$.data.tafsirs[0].from").value("1:2"))
                .andExpect(jsonPath("$.data.tafsirs[0].to").value("1:4"))
                .andExpect(jsonPath("$.data.tafsirs[0].text").value("<p>TEST ONLY 1:2-4</p>"))
                .andExpect(jsonPath("$.data.credit.name").value("Quran data provided by Quran Foundation"));
        mockMvc.perform(get("/v1/tafsirs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tafsirs[0].id").value(14))
                .andExpect(jsonPath("$.data.tafsirs[0].nameAr").value("TEST ONLY — تفسير 14"));
        mockMvc.perform(get("/v1/surahs/2/ayahs/1/tafsirs"))
                .andExpect(jsonPath("$.data.tafsirs[0].text").value("TEST ONLY row update"));
        mockMvc.perform(get("/v1/tafsirs/999/surahs/1/ayahs/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND_TAFSIR"));

        sync.run();

        assertThat(jdbcTemplate.queryForObject("select sync_token from content_sync where code = 'QF_TAFSIRS'",
                String.class)).isEqualTo("token-2");
        assertThat(count("select count(*) from tafsir_text where source_record_id = 9003")).isZero();
        assertThat(sync.isDue()).isFalse();
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
            server.createContext("/oauth2/token", e -> {
                String auth = e.getRequestHeaders().getFirst("Authorization");
                boolean ok = "POST".equals(e.getRequestMethod()) && auth != null && auth.startsWith("Basic ");
                write(e, ok ? 200 : 401, "{\"access_token\":\"test-token\",\"token_type\":\"bearer\",\"expires_in\":3600}");
            });
            server.createContext("/content", TafsirSyncIntegrationTest::content);
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
        if (!"test-token".equals(e.getRequestHeaders().getFirst("x-auth-token"))
                || !"test-client".equals(e.getRequestHeaders().getFirst("x-client-id"))) {
            write(e, 401, "{\"message\":\"unauthorized\",\"type\":\"unauthorized\",\"success\":false}");
            return;
        }
        AUTHORISED.incrementAndGet();
        String path = e.getRequestURI().getPath();
        String query = e.getRequestURI().getRawQuery() == null ? "" : e.getRequestURI().getRawQuery();
        if (path.endsWith("/resources/tafsirs")) {
            boolean arabic = query.contains("language=ar");
            StringBuilder list = new StringBuilder("{\"tafsirs\":[");
            for (int i = 0; i < CHOSEN.size(); i++) {
                int id = CHOSEN.get(i);
                list.append(i == 0 ? "" : ",").append("{\"id\":").append(id).append(",\"name\":\"TEST ONLY tafsir ")
                        .append(id).append("\",\"author_name\":\"TEST ONLY\",\"slug\":\"t").append(id)
                        .append("\",\"language_name\":\"arabic\",\"translated_name\":{\"name\":\"")
                        .append(arabic ? "TEST ONLY — تفسير " + id : "TEST ONLY tafsir " + id)
                        .append("\",\"language_name\":\"").append(arabic ? "arabic" : "english").append("\"}}");
            }
            write(e, 200, list.append("]}").toString());
        } else if (path.endsWith("/resources/sync") && query.contains("bootstrap=true")) {
            write(e, 200, "{\"sync\":{\"sync_until_sequence\":2,\"has_more\":true,\"next_page_url\":"
                    + "\"/api/v4/resources/sync?cursor=page-2&per_page=100\",\"mutations\":[{\"sequence\":1,"
                    + "\"type\":\"RESOURCE_CREATE\",\"resource_group\":\"tafsirs\",\"resource_id\":14,"
                    + "\"snapshot_url\":\"/api/v4/resources/snapshots/tafsirs/14\",\"data\":null}]}}");
        } else if (path.endsWith("/resources/sync") && query.contains("cursor=page-2")) {
            write(e, 200, "{\"sync\":{\"has_more\":false,\"next_sync_token\":\"token-1\",\"mutations\":[{\"sequence\":2,"
                    + "\"type\":\"ROW_UPDATE\",\"resource_group\":\"tafsirs\",\"resource_id\":14,\"record_key\":\"9003\","
                    + "\"data\":{\"id\":9003,\"verse_key\":\"2:1\",\"text\":\"TEST ONLY row update\"}}]}}");
        } else if (path.endsWith("/resources/sync") && query.contains("sync_token=token-1")) {
            write(e, 200, "{\"sync\":{\"has_more\":false,\"next_sync_token\":\"token-2\",\"mutations\":[{\"sequence\":3,"
                    + "\"type\":\"ROW_DELETE\",\"resource_group\":\"tafsirs\",\"resource_id\":14,\"record_key\":\"9003\","
                    + "\"data\":null}]}}");
        } else if (path.endsWith("/resources/snapshots/tafsirs/14")) {
            write(e, 200, "{\"resource_group\":\"tafsirs\",\"resource_id\":14,\"resource_content_id\":14,"
                    + "\"schema_version\":1,\"sync_sequence\":1,\"records\":["
                    + "{\"id\":9001,\"verse_key\":\"1:1\",\"group_verse_key_from\":\"1:1\",\"group_verse_key_to\":\"1:1\","
                    + "\"text\":\"<p>TEST ONLY 1:1</p>\"},"
                    + "{\"id\":9002,\"verse_key\":\"1:2\",\"group_verse_key_from\":\"1:2\",\"group_verse_key_to\":\"1:4\","
                    + "\"text\":\"<p>TEST ONLY 1:2-4</p>\"}]}");
        } else {
            write(e, 404, "{\"message\":\"not found\",\"type\":\"not_found\",\"success\":false}");
        }
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
