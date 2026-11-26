package io.github.omarsherifsayed.tajweed.business.reciter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.omarsherifsayed.tajweed.TestcontainersConfiguration;
import java.io.IOException;
import java.io.InputStream;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Proves, on a real PostgreSQL 18, that the reciter catalogue was imported as chosen with the author and that the
 * endpoints serve it: every recitation with its riwayah said plainly (listening only when it is not Hafs), the
 * surahs of the incomplete ones, the sources in order, and mp3quran's timings stored exactly as in its files — with
 * the reviewed failing surahs left out.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ReciterIntegrationTest {

    /** Reads the database directly. */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Sends requests to the application without a server. */
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("13 reciters; every non-Hafs recitation is listening only; the incomplete ones list their surahs")
    void importsTheCatalogue() {
        assertThat(count("select count(*) from reciter")).isEqualTo(13);
        assertThat(count("select count(*) from recitation r join riwayah w on w.pk_id = r.riwayah_id "
                + "where not w.matches_displayed_text")).isEqualTo(7);
        assertThat(count("select surah_count from recitation where mp3quran_moshaf_id = 103")).isEqualTo(38);
        assertThat(count("select surah_count from recitation where mp3quran_moshaf_id = 10924")).isEqualTo(110);
        assertThat(count("select count(*) from recitation_surah s join recitation r on r.pk_id = s.recitation_id "
                + "where r.mp3quran_moshaf_id = 112")).isEqualTo(114);
        assertThat(count("select count(*) from recitation where provider = 'MP3QURAN'")).isEqualTo(27);
        assertThat(count("select count(*) from content_source")).isEqualTo(21);
        assertThat(count("select count(*) from recitation")).isEqualTo(38);
    }

    @Test
    @DisplayName("Recitation 38: EveryAyah's «Hudhaify», identified by the author as Ali al-Hudhaifi — Hafs, its own recitation")
    void addsHudhaify() {
        assertThat(count("select count(*) from recitation r join reciter c on c.pk_id = r.reciter_id "
                + "where r.pk_id = 38 and c.mp3quran_id = 74 and r.provider = 'EVERYAYAH' and r.riwayah_id = 1")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForList("select url_template from audio_source where recitation_id = 38 "
                + "order by priority", String.class)).containsExactly(
                "https://everyayah.com/data/Hudhaify_128kbps/{surah}{ayah}.mp3",
                "https://everyayah.com/data/Hudhaify_64kbps/{surah}{ayah}.mp3",
                "https://everyayah.com/data/Hudhaify_32kbps/{surah}{ayah}.mp3");
        assertThat(count("select count(*) from recitation_surah where recitation_id = 38")).isEqualTo(114);
    }

    @Test
    @DisplayName("Every stored timing of al-Minshawi's murattal, Al-Baqarah, equals mp3quran's file")
    void storesTimingsVerbatim() throws IOException {
        List<String> expected = new ArrayList<>();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("content/mp3quran/timings/112/002.json")) {
            assertThat(in).isNotNull();
            for (JsonNode r : JsonMapper.builder().build().readTree(in.readAllBytes())) {
                expected.add(r.get("ayah").asInt() + "|" + r.get("start_time").asInt() + "|" + r.get("end_time").asInt());
            }
        }
        List<String> stored = jdbcTemplate.queryForList("select t.ayah_number || '|' || t.start_ms || '|' || t.end_ms "
                + "from ayah_timing t join recitation r on r.pk_id = t.recitation_id "
                + "where r.mp3quran_moshaf_id = 112 and t.surah_id = 2 order by t.ayah_number", String.class);
        assertThat(stored).hasSize(287).isEqualTo(expected);
    }

    @Test
    @DisplayName("GET /v1/reciters: al-Husary's Warsh recitation says «ورش عن نافع», its rawi and qari, listening only")
    void saysTheRiwayah() throws Exception {
        int reciter = count("select pk_id from reciter where mp3quran_id = 118");
        mockMvc.perform(get("/v1/reciters/" + reciter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recitations[0].riwayah.nameAr").value("حفص عن عاصم"))
                .andExpect(jsonPath("$.data.recitations[0].listeningOnly").value(false))
                .andExpect(jsonPath("$.data.recitations[?(@.riwayah.id == 2)].riwayah.rawiAr").value("ورش"))
                .andExpect(jsonPath("$.data.recitations[?(@.riwayah.id == 2)].riwayah.qariAr").value("نافع"))
                .andExpect(jsonPath("$.data.recitations[?(@.riwayah.id == 2)].listeningOnly").value(true));
        mockMvc.perform(get("/v1/reciters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(13));
    }

    @Test
    @DisplayName("GET /v1/recitations/{id}: the surahs, the sources in order and the credits")
    void servesARecitation() throws Exception {
        int id = count("select pk_id from recitation where mp3quran_moshaf_id = 112");
        mockMvc.perform(get("/v1/recitations/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.surahs.length()").value(114))
                .andExpect(jsonPath("$.data.sources[0].priority").value(1))
                .andExpect(jsonPath("$.data.sources[0].provider").value("MP3QURAN"))
                .andExpect(jsonPath("$.data.sources[0].urlTemplate")
                        .value("https://cdn.mp3quran.net/audio/muhammad-minshawi/r1/{surah}.mp3"))
                .andExpect(jsonPath("$.data.sources[0].downloadAllowed").value(false))
                .andExpect(jsonPath("$.data.credits[0].code").value("MP3QURAN_CATALOGUE"));
    }

    @Test
    @DisplayName("Timings: Al-Fatihah of al-Minshawi's murattal has 8 rows; his at-Tawbah (excluded) answers 404")
    void servesTimings() throws Exception {
        int id = count("select pk_id from recitation where mp3quran_moshaf_id = 112");
        mockMvc.perform(get("/v1/recitations/" + id + "/surahs/1/timings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.timings.length()").value(8))
                .andExpect(jsonPath("$.data.timings[0].ayah").value(0))
                .andExpect(jsonPath("$.data.source.code").value("MP3QURAN_TIMINGS"));
        mockMvc.perform(get("/v1/recitations/" + id + "/surahs/9/timings"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND_TIMINGS"));
        mockMvc.perform(get("/v1/recitations/" + id + "/surahs/115/timings"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/v1/recitations/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND_RECITATION"));
        mockMvc.perform(get("/v1/reciters/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND_RECITER"));
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
