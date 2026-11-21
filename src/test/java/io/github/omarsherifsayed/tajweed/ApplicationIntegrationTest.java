package io.github.omarsherifsayed.tajweed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * Proves that the whole application starts on a real PostgreSQL 18: Flyway applies the migrations, Hibernate accepts
 * the schema, the health check is up and {@code GET /v1/app-info} answers. It also writes the OpenAPI contract to
 * {@code docs/api/openapi.yaml}; CI fails when that file differs from the committed one.
 *
 * <p>Tagged {@code integration}: run with {@code ./gradlew integrationTest} (Docker must be running).
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApplicationIntegrationTest {

    /** Where the OpenAPI contract is kept, relative to the project folder. */
    private static final Path OPENAPI_FILE = Path.of("docs", "api", "openapi.yaml");

    /** Sends requests to the running application without a server. */
    @Autowired
    private MockMvc mockMvc;

    /** Reads the database directly. */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Flyway applied the baseline migration")
    void appliesTheMigrations() {
        Integer applied = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where version = '1.0.0' and success", Integer.class);
        assertThat(applied).isEqualTo(1);
    }

    @Test
    @DisplayName("The health check is UP and app-info answers in English")
    void startsAndAnswers() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/v1/app-info").header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Tajweed - The Noble Quran and Its Sciences"));
    }

    @Test
    @DisplayName("The OpenAPI contract is generated and written to docs/api/openapi.yaml")
    void writesTheOpenApiContract() throws Exception {
        String yaml = mockMvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(yaml).contains("/v1/app-info").contains("Accept-Language");
        Files.createDirectories(OPENAPI_FILE.getParent());
        Files.writeString(OPENAPI_FILE, yaml, StandardCharsets.UTF_8);
    }
}
