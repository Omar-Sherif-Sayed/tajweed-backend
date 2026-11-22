package io.github.omarsherifsayed.tajweed.business.tafsir.scheduler;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Looks once a day whether the tafsir sync is due (the credentials set, and the last sync older than 6 days or never
 * finished) and runs it, on one instance only (ShedLock). Off when {@code tajweed.quran-foundation.enabled} is
 * {@code false}.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tajweed.quran-foundation", name = "enabled", havingValue = "true")
public class TafsirSyncScheduler {

    /** Does the work. */
    private final TafsirSync sync;

    /**
     * The daily look.
     */
    @Scheduled(cron = "${tajweed.quran-foundation.cron}", zone = "UTC")
    @SchedulerLock(name = "tafsir-sync", lockAtLeastFor = "PT5M", lockAtMostFor = "PT6H")
    public void run() {
        if (sync.isDue()) {
            sync.run();
        }
    }
}
