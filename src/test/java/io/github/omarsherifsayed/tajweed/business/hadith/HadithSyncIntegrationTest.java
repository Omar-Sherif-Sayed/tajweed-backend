package io.github.omarsherifsayed.tajweed.business.hadith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.omarsherifsayed.tajweed.TestcontainersConfiguration;
import io.github.omarsherifsayed.tajweed.business.hadith.scheduler.HadeethEncSync;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
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
 * Proves the HadeethEnc sync and the hadith endpoints against a small local server that answers like HadeethEnc's API
 * — with placeholder texts marked «TEST ONLY», never their content (the author, 2026-11-22: their content is never in a
 * repository). It proves: the categories and titles are stored, the hadiths are listed from the root categories, each
 * text is stored exactly as received in Arabic and in a published translation, an incomplete answer is refused, a
 * language we do not support is ignored, and the endpoints fall back to Arabic where a translation is missing.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class HadithSyncIntegrationTest {

    /** The local server answering like HadeethEnc. */
    private static final HttpServer SERVER = start();

    /** Runs the sync. */
    @Autowired
    private HadeethEncSync sync;

    /** Reads the database directly. */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Sends requests to the application without a server. */
    @Autowired
    private MockMvc mockMvc;

    /**
     * Points the sync at the local server, with no pause between requests.
     *
     * @param registry the test's properties
     */
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("tajweed.hadeethenc.base-url", () -> "http://127.0.0.1:" + SERVER.getAddress().getPort());
        registry.add("tajweed.hadeethenc.delay", () -> "0s");
    }

    /**
     * Stops the local server.
     */
    @AfterAll
    static void stop() {
        SERVER.stop(0);
    }

    @Test
    @DisplayName("The sync stores categories, titles and texts as received; an incomplete answer is refused")
    void syncsAndServes() throws Exception {
        int written = sync.run();

        assertThat(written).isEqualTo(3);
        assertThat(count("select count(*) from hadeethenc_category")).isEqualTo(2);
        assertThat(count("select count(*) from hadeethenc_category_title")).isEqualTo(4);
        assertThat(count("select count(*) from hadeethenc_hadith")).isEqualTo(3);
        assertThat(count("select count(*) from hadeethenc_hadith_text where hadith_id = 103")).isZero();
        assertThat(jdbcTemplate.queryForObject("select hadeeth from hadeethenc_hadith_text where hadith_id = 101 "
                + "and language = 'ar'", String.class)).isEqualTo("TEST ONLY — نص تجريبي ١٠١، ليس حديثًا");
        assertThat(jdbcTemplate.queryForObject("select status from content_sync where code = 'HADEETHENC'", String.class))
                .isEqualTo("OK");

        mockMvc.perform(get("/v1/hadiths/101").header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.arabic.grade").value("TEST ONLY — درجة"))
                .andExpect(jsonPath("$.data.translation.language").value("en"))
                .andExpect(jsonPath("$.data.translation.hints[0]").value("TEST ONLY — benefit"))
                .andExpect(jsonPath("$.data.arabic.wordsMeanings[0].word").value("كلمة"))
                .andExpect(jsonPath("$.data.sourceUrl").value("https://hadeethenc.com/en/browse/hadith/101"))
                .andExpect(jsonPath("$.data.credit.url").value("https://hadeethenc.com"));
        mockMvc.perform(get("/v1/hadiths/102").header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.translation").doesNotExist())
                .andExpect(jsonPath("$.data.sourceUrl").value("https://hadeethenc.com/ar/browse/hadith/102"));
        mockMvc.perform(get("/v1/hadiths/103"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND_HADITH"));
        mockMvc.perform(get("/v1/hadith-categories").header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories[0].title").value("TEST ONLY — category"))
                .andExpect(jsonPath("$.data.categories[0].hadithCount").value(2));
        mockMvc.perform(get("/v1/hadith-categories/1/hadiths").header("Accept-Language", "fr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.hadiths[0].titleLanguage").value("ar"));
        mockMvc.perform(get("/v1/hadith-categories/77/hadiths"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND_HADITH_CATEGORY"));
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
     * Starts the local server. Hadith 101 is in Arabic and English, 102 in Arabic only, 103's answer has no grade.
     *
     * @return the server
     */
    private static HttpServer start() {
        Map<String, String> answers = Map.ofEntries(
                Map.entry("/languages", "[{\"code\":\"ar\",\"native\":\"العربية\"},{\"code\":\"en\",\"native\":\"English\"},"
                        + "{\"code\":\"ug\",\"native\":\"x\"}]"),
                Map.entry("/categories/list/?language=ar", "[{\"id\":\"1\",\"title\":\"TEST ONLY — تصنيف\","
                        + "\"hadeeths_count\":\"3\",\"parent_id\":null},{\"id\":\"2\",\"title\":\"TEST ONLY — تصنيف فرعي\","
                        + "\"hadeeths_count\":\"2\",\"parent_id\":\"1\"}]"),
                Map.entry("/categories/list/?language=en", "[{\"id\":\"1\",\"title\":\"TEST ONLY — category\","
                        + "\"hadeeths_count\":\"3\",\"parent_id\":null},{\"id\":\"2\",\"title\":\"TEST ONLY — sub-category\","
                        + "\"hadeeths_count\":\"2\",\"parent_id\":\"1\"}]"),
                Map.entry("/hadeeths/list/?language=ar&category_id=1&page=1&per_page=100", "{\"data\":["
                        + "{\"id\":\"101\",\"title\":\"t\",\"translations\":[\"ar\",\"en\",\"ug\"]},"
                        + "{\"id\":\"102\",\"title\":\"t\",\"translations\":[\"ar\"]},"
                        + "{\"id\":\"103\",\"title\":\"t\",\"translations\":[\"ar\"]}],"
                        + "\"meta\":{\"current_page\":\"1\",\"last_page\":1,\"total_items\":3,\"per_page\":\"100\"}}"),
                Map.entry("/hadeeths/one/?language=ar&id=101", hadith(101, "TEST ONLY — نص تجريبي ١٠١، ليس حديثًا",
                        "TEST ONLY — درجة", "[\"2\"]")),
                Map.entry("/hadeeths/one/?language=en&id=101", hadith(101, "TEST ONLY — text 101, not a hadith",
                        "TEST ONLY — grade", "[\"2\"]")),
                Map.entry("/hadeeths/one/?language=ar&id=102", hadith(102, "TEST ONLY — نص تجريبي ١٠٢",
                        "TEST ONLY — درجة", "[\"2\"]")),
                Map.entry("/hadeeths/one/?language=ar&id=103", "{\"id\":\"103\",\"title\":\"TEST ONLY\","
                        + "\"hadeeth\":\"TEST ONLY — no grade\",\"categories\":[\"2\"]}"));
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> answer(exchange, answers));
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Answers one request from the fixed answers, or {@code 404}.
     *
     * @param exchange the request
     * @param answers  path and query → answer
     * @throws IOException if the answer cannot be written
     */
    private static void answer(HttpExchange exchange, Map<String, String> answers) throws IOException {
        String query = exchange.getRequestURI().getRawQuery();
        String key = exchange.getRequestURI().getPath() + (query == null ? "" : "?" + query);
        String body = answers.get(key);
        byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(body == null ? 404 : 200, body == null ? -1 : bytes.length);
        if (body != null) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
        exchange.close();
    }

    /**
     * A placeholder answer shaped like {@code hadeeths/one}.
     *
     * @param id         the id
     * @param text       the placeholder text
     * @param grade      the placeholder grade
     * @param categories the categories as a JSON array
     * @return the JSON
     */
    private static String hadith(int id, String text, String grade, String categories) {
        return "{\"id\":\"" + id + "\",\"title\":\"TEST ONLY — title " + id + "\",\"hadeeth\":\"" + text + "\","
                + "\"attribution\":\"TEST ONLY\",\"grade\":\"" + grade + "\",\"explanation\":\"TEST ONLY\","
                + "\"hints\":[\"TEST ONLY — benefit\"],\"categories\":" + categories + ",\"translations\":[\"ar\",\"en\"],"
                + "\"hadeeth_intro\":\"\",\"words_meanings\":[{\"word\":\"كلمة\",\"meaning\":\"TEST ONLY\"}],"
                + "\"reference\":\"TEST ONLY — reference\"}";
    }
}
