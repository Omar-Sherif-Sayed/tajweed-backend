package io.github.omarsherifsayed.tajweed.business.hadith.scheduler;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Looks once a day whether the HadeethEnc sync is due (never finished, or older than 30 days) and runs it, on one
 * instance only (ShedLock). A first full sync takes about a day (one request a second), hence the long lock. Off when
 * {@code tajweed.hadeethenc.enabled} is {@code false} (the tests).
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tajweed.hadeethenc", name = "enabled", havingValue = "true")
public class HadeethEncSyncScheduler {

    /** Does the work. */
    private final HadeethEncSync sync;

    /**
     * The daily look.
     */
    @Scheduled(cron = "${tajweed.hadeethenc.cron}", zone = "UTC")
    @SchedulerLock(name = "hadeethenc-sync", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30H")
    public void run() {
        if (sync.isDue()) {
            sync.run();
        }
    }
}
