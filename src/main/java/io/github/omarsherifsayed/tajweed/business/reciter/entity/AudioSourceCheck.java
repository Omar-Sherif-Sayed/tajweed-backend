package io.github.omarsherifsayed.tajweed.business.reciter.entity;

import io.github.omarsherifsayed.tajweed.business.reciter.enums.SourceStatus;
import io.github.omarsherifsayed.tajweed.general.audit.AbstractAuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;

/**
 * The health of an audio source (table {@code audio_source_check}) — run-time state written by the daily
 * check, never content.
 */
@Getter
@Entity
@Table(name = "audio_source_check")
public class AudioSourceCheck extends AbstractAuditingEntity {

    /** The audio source this state belongs to. */
    @Id
    @Column(name = "audio_source_id")
    private Integer id;

    /** The result. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SourceStatus status;

    /** Failed checks in a row. */
    @Column(name = "consecutive_failures")
    private Short consecutiveFailures;

    /** When it was last checked. */
    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    /** When it last answered with audio, or {@code null}. */
    @Column(name = "last_ok_at")
    private Instant lastOkAt;

    /** Failed checks in a row after which a source is marked {@link SourceStatus#FAILING}. */
    public static final int FAILURES_BEFORE_FAILING = 3;

    /**
     * The state of a source that was never checked.
     *
     * @param audioSourceId the source
     */
    public AudioSourceCheck(Integer audioSourceId) {
        this.id = audioSourceId;
        this.status = SourceStatus.OK;
        this.consecutiveFailures = 0;
    }

    /**
     * Records the result of one check: an answer with audio resets the count; the third failure in a row marks the
     * source {@link SourceStatus#FAILING}.
     *
     * @param ok whether the sample file answered {@code 200} with audio
     * @param at when it was checked
     */
    public void record(boolean ok, Instant at) {
        lastCheckedAt = at;
        if (ok) {
            consecutiveFailures = 0;
            lastOkAt = at;
            status = SourceStatus.OK;
        } else {
            consecutiveFailures = (short) Math.min(consecutiveFailures + 1, Short.MAX_VALUE);
            if (consecutiveFailures >= FAILURES_BEFORE_FAILING) {
                status = SourceStatus.FAILING;
            }
        }
    }

    /**
     * For JPA only.
     */
    protected AudioSourceCheck() {
        // filled by Hibernate
    }
}
