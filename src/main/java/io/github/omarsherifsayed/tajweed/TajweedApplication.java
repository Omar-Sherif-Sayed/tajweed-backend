package io.github.omarsherifsayed.tajweed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * The entry point of the Tajweed backend.
 *
 * <p>Run it locally with {@code ./gradlew bootRun}: Spring Boot starts the PostgreSQL of {@code compose.yaml} in
 * Docker by itself and connects to it. In production the database comes from the {@code SPRING_DATASOURCE_*}
 * environment variables (see {@code README.md}).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class TajweedApplication {

    /**
     * Not instantiated by hand: Spring Boot creates the application.
     */
    protected TajweedApplication() {
        // Spring Boot needs a non-private constructor; nothing to do here.
    }

    /**
     * Starts the application.
     *
     * @param args command-line arguments, passed to Spring Boot unchanged
     */
    public static void main(String[] args) {
        SpringApplication.run(TajweedApplication.class, args);
    }
}
