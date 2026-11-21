package io.github.omarsherifsayed.tajweed.business.reciter.scheduler;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the health check of the audio sources once a day (P1-B6), on one instance only (ShedLock, table
 * {@code shedlock}). Off when {@code tajweed.audio-health.enabled} is {@code false} (the tests).
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tajweed.audio-health", name = "enabled", havingValue = "true")
public class AudioSourceHealthScheduler {

    /** Does the work. */
    private final AudioSourceHealthChecker checker;

    /**
     * The daily run. The lock is held at least five minutes, so a second instance whose clock is a little behind does
     * not run it again, and at most an hour.
     */
    @Scheduled(cron = "${tajweed.audio-health.cron}", zone = "UTC")
    @SchedulerLock(name = "audio-source-health", lockAtLeastFor = "PT5M", lockAtMostFor = "PT1H")
    public void run() {
        checker.checkAll();
    }
}
