package io.github.omarsherifsayed.tajweed.general.config;

import java.time.Clock;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Scheduled jobs ({@code AGENTS.md} §9: the ShedLock pattern): Spring's scheduler, and a lock in the database table
 * {@code shedlock} so that a job runs on one instance only, even when two run behind a load balancer. The lock uses
 * the database's clock, so the instances' clocks need not agree.
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT1H")
public class SchedulingConfig {

    /**
     * Created by Spring.
     */
    public SchedulingConfig() {
        // no state
    }

    /**
     * The lock provider on the application's database.
     *
     * @param dataSource the database
     * @return ShedLock's JDBC provider, using the database time
     */
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .usingDbTime()
                .build());
    }

    /**
     * The clock of the application (UTC); tests may replace it.
     *
     * @return the system clock in UTC
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
