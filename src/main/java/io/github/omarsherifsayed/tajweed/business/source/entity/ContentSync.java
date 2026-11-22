package io.github.omarsherifsayed.tajweed.business.source.entity;

import io.github.omarsherifsayed.tajweed.general.audit.AbstractAuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;

/**
 * The state of one sync of content fetched at run time (table {@code content_sync}) — HadeethEnc's hadiths, the
 * Quran Foundation's tafsirs: when it ran, how it ended, and the cursor to continue from. Run-time state, never
 * content.
 */
@Getter
@Entity
@Table(name = "content_sync")
public class ContentSync extends AbstractAuditingEntity {

    /** A sync is running (or stopped without finishing). */
    public static final String RUNNING = "RUNNING";

    /** The last run finished. */
    public static final String OK = "OK";

    /** The last run stopped on an error; the stored content is kept. */
    public static final String FAILED = "FAILED";

    /** The longest message kept. */
    private static final int MESSAGE_LENGTH = 500;

    /** The primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pk_id")
    private Integer id;

    /** What is synced, for example {@code HADEETHENC} or {@code QF_TAFSIRS}. */
    @Column(name = "code")
    private String code;

    /** {@link #RUNNING}, {@link #OK} or {@link #FAILED}. */
    @Column(name = "status")
    private String status;

    /** When the last run started. */
    @Column(name = "last_started_at")
    private Instant lastStartedAt;

    /** When the last run ended, or {@code null} while it runs. */
    @Column(name = "last_finished_at")
    private Instant lastFinishedAt;

    /** When a run last finished without error, or {@code null}. */
    @Column(name = "last_success_at")
    private Instant lastSuccessAt;

    /** How many items the last run wrote. */
    @Column(name = "items")
    private Integer items;

    /** The provider's cursor to continue from (the Quran Foundation's {@code sync_token}), or {@code null}. */
    @Column(name = "sync_token")
    private String syncToken;

    /** What happened, in a few words (an error's message), or {@code null}. */
    @Column(name = "message")
    private String message;

    /**
     * A sync that has never run.
     *
     * @param code what is synced
     */
    public ContentSync(String code) {
        this.code = code;
        this.items = 0;
    }

    /**
     * Records the start of a run.
     *
     * @param at when it started
     */
    public void start(Instant at) {
        status = RUNNING;
        lastStartedAt = at;
        lastFinishedAt = null;
        message = null;
    }

    /**
     * Records a run that finished.
     *
     * @param written how many items it wrote
     * @param token   the cursor to continue from next time, or {@code null}
     * @param at      when it finished
     */
    public void succeed(int written, String token, Instant at) {
        status = OK;
        items = written;
        syncToken = token;
        lastFinishedAt = at;
        lastSuccessAt = at;
    }

    /**
     * Records a run that stopped on an error; the cursor and the stored content are kept.
     *
     * @param written how many items it wrote before the error
     * @param error   what went wrong
     * @param at      when it stopped
     */
    public void fail(int written, String error, Instant at) {
        status = FAILED;
        items = written;
        lastFinishedAt = at;
        message = error == null ? null : error.substring(0, Math.min(error.length(), MESSAGE_LENGTH));
    }

    /**
     * Forgets the cursor, so the next run starts from the beginning.
     */
    public void forgetToken() {
        syncToken = null;
    }

    /**
     * For JPA only.
     */
    protected ContentSync() {
        // filled by Hibernate
    }
}
