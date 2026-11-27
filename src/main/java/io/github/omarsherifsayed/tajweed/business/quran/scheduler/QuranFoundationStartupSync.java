package io.github.omarsherifsayed.tajweed.business.quran.scheduler;

import io.github.omarsherifsayed.tajweed.business.tafsir.scheduler.TafsirSync;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Runs the Quran Foundation syncs that are due once the application has started — the mushaf's glyphs, then the
 * tafsirs — on a background thread, so a new database (a developer's, or a new server's) is filled without waiting
 * for the nightly schedule. On only when {@code tajweed.quran-foundation.sync-on-start} is {@code true}
 * ({@code QF_SYNC_ON_START}); meant for one instance, as it takes no ShedLock lock.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tajweed.quran-foundation", name = "sync-on-start", havingValue = "true")
public class QuranFoundationStartupSync {

    /** The mushaf's glyphs. */
    private final QfMushafSync mushafSync;

    /** The tafsirs. */
    private final TafsirSync tafsirSync;

    /**
     * Starts the due syncs in the background.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        Thread.ofVirtual().name("qf-startup-sync").start(() -> {
            if (mushafSync.isDue()) {
                mushafSync.run();
            }
            if (tafsirSync.isDue()) {
                tafsirSync.run();
            }
        });
        log.info("Quran Foundation syncs started in the background (sync-on-start)");
    }
}
