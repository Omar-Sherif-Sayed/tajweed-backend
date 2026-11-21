package io.github.omarsherifsayed.tajweed.business.quran;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.omarsherifsayed.tajweed.TestcontainersConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Proves, on a real PostgreSQL 18, that the Quran was imported correctly and is served unchanged ({@code AGENTS.md}
 * §6): 114 surahs and 6,236 ayahs; <b>every ayah's text equal, character for character, to the Tanzil file</b>; 604
 * pages, 30 juz, 240 quarters with quarter 106 starting at 15:49 as printed; the basmala at the head of 112 first
 * ayahs; 15 sajdah places; the five sources recorded with their checksums (two of them the search text) — and that the endpoints answer with that
 * data, or with the right error.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class QuranImportIntegrationTest {

    /** Reads the database directly. */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Sends requests to the application without a server. */
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Every one of the 6,236 ayahs is stored exactly as the Tanzil file writes it")
    void storesEveryAyahVerbatim() throws IOException {
        // given: the source file's lines, in order
        List<String> expected = new ArrayList<>();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("content/tanzil/quran-uthmani.txt")) {
            assertThat(in).isNotNull();
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                if (!line.isEmpty() && !line.startsWith("#")) {
                    expected.add(line);
                }
            }
        }

        // when: the stored rows, in the mushaf's order
        List<String> stored = jdbcTemplate.queryForList(
                "select surah_id || '|' || number_in_surah || '|' || text_uthmani from ayah order by pk_id",
                String.class);

        // then
        assertThat(stored).hasSize(6236).isEqualTo(expected);
    }

    @Test
    @DisplayName("The counts and the divisions of the mushaf are right")
    void dividesTheMushaf() {
        assertThat(count("select count(*) from surah")).isEqualTo(114);
        assertThat(count("select sum(ayah_count) from surah")).isEqualTo(6236);
        assertThat(count("select count(distinct page) from ayah")).isEqualTo(604);
        assertThat(count("select count(distinct juz) from ayah")).isEqualTo(30);
        assertThat(count("select count(distinct hizb_quarter) from ayah")).isEqualTo(240);
        assertThat(count("select count(*) from ayah where basmala_word_count = 4")).isEqualTo(112);
        assertThat(count("select count(*) from ayah where sajdah_type is not null")).isEqualTo(15);
        assertThat(count("select count(*) from content_source")).isEqualTo(5);
    }

    @Test
    @DisplayName("Quarter 106 starts at 15:49, as the printed mushaf has it (Tanzil's metadata says 15:50)")
    void appliesTheReviewedQuarterCorrection() {
        String first = jdbcTemplate.queryForObject(
                "select surah_id || ':' || number_in_surah from ayah where hizb_quarter = 106 order by pk_id limit 1",
                String.class);
        assertThat(first).isEqualTo("15:49");
    }

    @Test
    @DisplayName("GET /v1/surahs lists the 114 surahs with their first page")
    void listsTheSurahs() throws Exception {
        mockMvc.perform(get("/v1/surahs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(114))
                .andExpect(jsonPath("$.data[0].nameAr").value("الفاتحة"))
                .andExpect(jsonPath("$.data[0].revelationType").value("MECCAN"))
                .andExpect(jsonPath("$.data[0].startPage").value(1))
                .andExpect(jsonPath("$.data[1].ayahCount").value(286))
                .andExpect(jsonPath("$.data[1].startPage").value(2));
    }

    @Test
    @DisplayName("GET /v1/surahs/1/ayahs gives the 7 ayahs, the first one exactly as in the file, and the text's source")
    void servesASurah() throws Exception {
        String firstAyah = jdbcTemplate.queryForObject(
                "select text_uthmani from ayah where surah_id = 1 and number_in_surah = 1", String.class);
        mockMvc.perform(get("/v1/surahs/1/ayahs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.surah.number").value(1))
                .andExpect(jsonPath("$.data.ayahs.length()").value(7))
                .andExpect(jsonPath("$.data.ayahs[0].text").value(firstAyah))
                .andExpect(jsonPath("$.data.ayahs[0].basmalaWordCount").value(0))
                .andExpect(jsonPath("$.data.textSource.code").value("TANZIL_UTHMANI"))
                .andExpect(jsonPath("$.data.textSource.sourceUrl").value("https://tanzil.net/download/"));
    }

    @Test
    @DisplayName("GET /v1/surahs/2/ayahs/1 marks the basmala at the head of the text")
    void marksTheBasmala() throws Exception {
        mockMvc.perform(get("/v1/surahs/2/ayahs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.basmalaWordCount").value(4))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.juz").value(1))
                .andExpect(jsonPath("$.data.hizb").value(1));
    }

    @Test
    @DisplayName("An ayah the surah does not have answers 404 NOT_FOUND_AYAH; a surah out of range answers 400")
    void refusesWhatDoesNotExist() throws Exception {
        mockMvc.perform(get("/v1/surahs/1/ayahs/8").header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND_AYAH"))
                .andExpect(jsonPath("$.message").value("This ayah does not exist in this surah."));
        mockMvc.perform(get("/v1/surahs/115/ayahs"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mockMvc.perform(get("/v1/pages/605"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /v1/pages/604 gives the last three surahs; GET /v1/juzs gives 30 juz, juz 2 from 2:142")
    void servesPagesAndJuzs() throws Exception {
        mockMvc.perform(get("/v1/pages/604"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ayahs[0].surahNumber").value(112))
                .andExpect(jsonPath("$.data.ayahs[-1:].surahNumber").value(114));
        mockMvc.perform(get("/v1/juzs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(30))
                .andExpect(jsonPath("$.data[1].startSurahNumber").value(2))
                .andExpect(jsonPath("$.data[1].startAyahNumber").value(142));
    }

    @Test
    @DisplayName("GET /v1/content-version gives a 16-digit fingerprint and the five sources")
    void servesTheContentVersion() throws Exception {
        mockMvc.perform(get("/v1/content-version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(org.hamcrest.Matchers.matchesPattern("[0-9a-f]{16}")))
                .andExpect(jsonPath("$.data.sources.length()").value(5))
                .andExpect(jsonPath("$.data.sources[4].sha256")
                        .value("1862ee63c70fbfc6285e0bb0b9587b89beb5b7891109a4d543d9d2cf8283e8c8"))
                .andExpect(jsonPath("$.data.sources[0].sha256")
                        .value("6933e133dd56db778c801bf738848454e43648105a151e8d84d86a7cae39ec5f"));
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
}
