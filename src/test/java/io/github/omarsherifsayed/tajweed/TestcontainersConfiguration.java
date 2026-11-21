package io.github.omarsherifsayed.tajweed;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Gives integration tests a real PostgreSQL 18 in Docker (the same major version as production), started once and
 * connected to the application by {@code @ServiceConnection}. Docker must be running.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /**
     * The PostgreSQL container.
     *
     * @return a PostgreSQL 18 container
     */
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:18"));
    }
}
