package io.github.omarsherifsayed.tajweed.business.radio.scheduler;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the health check of the radio streams once a day, on one instance only (ShedLock), at the time of the audio
 * sources' check ({@code tajweed.audio-health.*}). Off when {@code tajweed.audio-health.enabled} is {@code false}.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tajweed.audio-health", name = "enabled", havingValue = "true")
public class RadioStreamHealthScheduler {

    /** Does the work. */
    private final RadioStreamHealthChecker checker;

    /**
     * The daily run; the lock is held at least five minutes and at most an hour.
     */
    @Scheduled(cron = "${tajweed.audio-health.cron}", zone = "UTC")
    @SchedulerLock(name = "radio-stream-health", lockAtLeastFor = "PT5M", lockAtMostFor = "PT1H")
    public void run() {
        checker.checkAll();
    }
}
