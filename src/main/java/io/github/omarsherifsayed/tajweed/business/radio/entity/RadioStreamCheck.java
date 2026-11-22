package io.github.omarsherifsayed.tajweed.business.radio.entity;

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
 * The health of a radio stream (table {@code radio_stream_check}) — run-time state written by the daily check, never
 * content. The same rule as the audio sources: the third failure in a row marks it {@link SourceStatus#FAILING}.
 */
@Getter
@Entity
@Table(name = "radio_stream_check")
public class RadioStreamCheck extends AbstractAuditingEntity {

    /** Failed checks in a row after which a stream is marked {@link SourceStatus#FAILING}. */
    public static final int FAILURES_BEFORE_FAILING = 3;

    /** The stream this state belongs to. */
    @Id
    @Column(name = "stream_id")
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

    /**
     * The state of a stream that was never checked.
     *
     * @param streamId the stream
     */
    public RadioStreamCheck(Integer streamId) {
        this.id = streamId;
        this.status = SourceStatus.OK;
        this.consecutiveFailures = 0;
    }

    /**
     * Records the result of one check.
     *
     * @param ok whether the stream answered {@code 200} with audio
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
    protected RadioStreamCheck() {
        // filled by Hibernate
    }
}
