package io.github.omarsherifsayed.tajweed.business.radio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.omarsherifsayed.tajweed.TestcontainersConfiguration;
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
 * Proves, on a real PostgreSQL 18, that the radio stations were imported as decided with the author (recitation only)
 * and that {@code /v1/radios} serves them: the channels of several reciters, the riwayah only where the channel names
 * it, a channel of one of our reciters linked to him, and no programme.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RadioIntegrationTest {

    /** Reads the database directly. */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Sends requests to the application without a server. */
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("133 stations kept (127 of one reciter, 6 of several), one stream each; no programme")
    void importsTheStations() {
        assertThat(count("select count(*) from radio_station")).isEqualTo(133);
        assertThat(count("select count(*) from radio_station where kind = 'RECITERS'")).isEqualTo(6);
        assertThat(count("select count(*) from radio_stream")).isEqualTo(133);
        assertThat(count("select count(*) from radio_station where provider_key in (113, 116, 21114, 109082)")).isZero();
        assertThat(count("select count(*) from radio_station where riwayah_id is not null")).isEqualTo(5);
    }

    @Test
    @DisplayName("Abdul Basit's Warsh channel names its riwayah and links to our reciter")
    void namesTheRiwayah() throws Exception {
        int station = count("select pk_id from radio_station where provider_key = 31");
        int reciter = count("select pk_id from reciter where mp3quran_id = 51");
        mockMvc.perform(get("/v1/radios/" + station))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stations[0].riwayah.nameAr").value("ورش عن نافع"))
                .andExpect(jsonPath("$.data.stations[0].reciterId").value(reciter))
                .andExpect(jsonPath("$.data.stations[0].streams[0].url")
                        .value("https://backup.qurango.net/radio/abdulbasit_abdulsamad_warsh"))
                .andExpect(jsonPath("$.data.credits[0].code").value("MP3QURAN_RADIOS"));
    }

    @Test
    @DisplayName("GET /v1/radios lists every station; an unknown one answers 404 NOT_FOUND_RADIO")
    void servesTheList() throws Exception {
        mockMvc.perform(get("/v1/radios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stations.length()").value(133));
        mockMvc.perform(get("/v1/radios/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND_RADIO"));
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
