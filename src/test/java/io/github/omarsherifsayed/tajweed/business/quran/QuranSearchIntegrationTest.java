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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Proves, on a real PostgreSQL 18, that the search text was imported verbatim beside every ayah and that
 * {@code GET /v1/quran-search} answers the three kinds of search ({@code AGENTS.md} §8.1, F16).
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class QuranSearchIntegrationTest {

    /** Reads the database directly. */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Sends requests to the application without a server. */
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Both search texts are stored exactly as the Tanzil files write them, beside the right ayah")
    void storesTheSearchTextVerbatim() throws IOException {
        List<String> stored = jdbcTemplate.queryForList("select a.surah_id || '|' || a.number_in_surah || '|' "
                + "|| s.text_simple || '|' || s.text_simple_clean from ayah_search_text s join ayah a "
                + "on a.pk_id = s.ayah_id order by s.ayah_id", String.class);
        List<String> simple = lines("content/tanzil/quran-simple.txt");
        List<String> clean = lines("content/tanzil/quran-simple-clean.txt");
        List<String> expected = new ArrayList<>();
        for (int i = 0; i < simple.size(); i++) {
            expected.add(simple.get(i) + "|" + clean.get(i).split("\\|", 3)[2]);
        }
        assertThat(stored).hasSize(6236).isEqualTo(expected);
    }

    @Test
    @DisplayName("A word: «الصبر» is in 4 ayahs, the first 2:45, with the displayed Uthmani text and the sources")
    void findsWords() throws Exception {
        String uthmani = jdbcTemplate.queryForObject(
                "select text_uthmani from ayah where surah_id = 2 and number_in_surah = 45", String.class);
        mockMvc.perform(get("/v1/quran-search").param("q", "الصبر"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(4))
                .andExpect(jsonPath("$.data.withTashkeel").value(false))
                .andExpect(jsonPath("$.data.hits[0].ayah.surahNumber").value(2))
                .andExpect(jsonPath("$.data.hits[0].ayah.number").value(45))
                .andExpect(jsonPath("$.data.hits[0].ayah.text").value(uthmani))
                .andExpect(jsonPath("$.data.textSource.code").value("TANZIL_UTHMANI"))
                .andExpect(jsonPath("$.data.searchSources[0].code").value("TANZIL_SIMPLE"))
                .andExpect(jsonPath("$.data.searchSources[1].code").value("TANZIL_SIMPLE_CLEAN"));
    }

    @Test
    @DisplayName("Pages of results: «الله» has 1,842 ayahs; page 1 of size 5 holds the 6th to the 10th")
    void pagesTheResults() throws Exception {
        mockMvc.perform(get("/v1/quran-search").param("q", "الله").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1842))
                .andExpect(jsonPath("$.data.hits.length()").value(5))
                .andExpect(jsonPath("$.data.countsBySurah[0].surahNumber").value(1));
    }

    @Test
    @DisplayName("With tashkeel: «مُحَمَّدٌ» is matched against the vocalised text — 2 ayahs, the first 3:144")
    void searchesWithTashkeel() throws Exception {
        mockMvc.perform(get("/v1/quran-search").param("q", "مُحَمَّدٌ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.withTashkeel").value(true))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.hits[0].ayah.surahNumber").value(3))
                .andExpect(jsonPath("$.data.hits[0].ayah.number").value(144));
    }

    @Test
    @DisplayName("A reference: «2:255» names Ayat al-Kursi; «البقرة» names surah 2")
    void findsAReferenceAndASurah() throws Exception {
        mockMvc.perform(get("/v1/quran-search").param("q", "2:255"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ayah.id").value(262))
                .andExpect(jsonPath("$.data.total").value(0));
        mockMvc.perform(get("/v1/quran-search").param("q", "البقرة"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.surahs[0].number").value(2));
        mockMvc.perform(get("/v1/quran-search").param("q", "2:300"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ayah").doesNotExist());
    }

    @Test
    @DisplayName("An empty query, one of marks only, or a size over 100 answers 400 INVALID_REQUEST")
    void refusesABadQuery() throws Exception {
        mockMvc.perform(get("/v1/quran-search").param("q", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mockMvc.perform(get("/v1/quran-search").param("q", "ـ ۚ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mockMvc.perform(get("/v1/quran-search").param("q", "الله").param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    /**
     * The {@code surah|ayah|text} lines of a file, comments skipped.
     *
     * @param path the file on the class path
     * @return the lines, in order
     * @throws IOException if it cannot be read
     */
    private List<String> lines(String path) throws IOException {
        List<String> out = new ArrayList<>();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(in).isNotNull();
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                if (!line.isEmpty() && !line.startsWith("#")) {
                    out.add(line);
                }
            }
        }
        return out;
    }
}
