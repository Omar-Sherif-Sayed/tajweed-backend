package io.github.omarsherifsayed.tajweed.business;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.omarsherifsayed.tajweed.TestcontainersConfiguration;
import io.github.omarsherifsayed.tajweed.business.hadith.client.HadeethEncClient;
import io.github.omarsherifsayed.tajweed.business.tafsir.scheduler.TafsirSync;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Checks the real external APIs — run by hand only ({@code ./gradlew liveTest}), never in CI: the Quran Foundation with
 * the author's credentials from the environment ({@code QF_CLIENT_ID}, {@code QF_CLIENT_SECRET}, {@code QF_ENV}), and
 * HadeethEnc's API. Nothing received is printed or kept in a file: only counts are printed.
 */
@Tag("live")
@SpringBootTest(properties = {"tajweed.hadeethenc.enabled=false", "tajweed.quran-foundation.enabled=false",
        "tajweed.quran-foundation.only-tafsirs=${QF_LIVE_TAFSIRS:}",
        "tajweed.audio-health.enabled=false"})
@Import(TestcontainersConfiguration.class)
class LiveSourcesTest {

    /** Runs the tafsir sync. */
    @Autowired
    private TafsirSync tafsirSync;

    /** Calls HadeethEnc. */
    @Autowired
    private HadeethEncClient hadeethEncClient;

    /** Reads the database directly. */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @EnabledIfEnvironmentVariable(named = "QF_CLIENT_ID", matches = ".+")
    @DisplayName("Quran Foundation: token, Content Sync bootstrap and snapshots of the chosen tafsirs; 1:1 covered")
    void syncsTheTafsirs() {
        int written = tafsirSync.run();

        Integer tafsirs = jdbcTemplate.queryForObject("select count(*) from tafsir", Integer.class);
        Integer covering = jdbcTemplate.queryForObject("select count(distinct tafsir_id) from tafsir_text "
                + "where ayah_id <= 1 and ayah_to_id >= 1 and text <> ''", Integer.class);
        String status = jdbcTemplate.queryForObject("select status || ' ' || coalesce(message, '') from content_sync "
                + "where code = 'QF_TAFSIRS'", String.class);
        System.out.println("LIVE Quran Foundation: " + written + " rows, " + tafsirs + " tafsirs, " + covering
                + " covering 1:1, status " + status);
        assertThat(status).startsWith("OK");
        assertThat(written).isPositive();
        assertThat(covering).isPositive();
    }

    @Test
    @DisplayName("HadeethEnc: the languages, the categories and one hadith answer in the shape the sync reads")
    void readsHadeethEnc() {
        JsonMapper json = JsonMapper.builder().build();
        JsonNode languages = json.readTree(hadeethEncClient.languages());
        JsonNode categories = json.readTree(hadeethEncClient.categories("ar"));
        JsonNode one = json.readTree(hadeethEncClient.hadith("ar", 2962));
        System.out.println("LIVE HadeethEnc: " + languages.size() + " languages, " + categories.size() + " categories");
        assertThat(languages.size()).isGreaterThan(20);
        assertThat(categories.size()).isGreaterThan(100);
        assertThat(one.path("id").asString()).isEqualTo("2962");
        assertThat(one.path("grade").asString()).isNotBlank();
        assertThat(one.path("hadeeth").asString()).isNotBlank();
    }
}
