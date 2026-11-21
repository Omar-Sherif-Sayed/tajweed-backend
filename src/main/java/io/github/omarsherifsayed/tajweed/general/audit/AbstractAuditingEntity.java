package io.github.omarsherifsayed.tajweed.general.audit;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * The base class of every database entity ({@code AGENTS.md} §9): when the row was created and when it last changed.
 *
 * <p>Hibernate fills both columns itself; the code never sets them. Every table therefore has the two columns
 * {@code created_at} and {@code updated_at} (type {@code timestamptz}), created by the table's Flyway migration.
 * The first entities extend it in phase 1 (the {@code quran} module).
 */
@Getter
@MappedSuperclass
public abstract class AbstractAuditingEntity {

    /** When the row was inserted (UTC). */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** When the row was last changed (UTC). */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * For subclasses only.
     */
    protected AbstractAuditingEntity() {
        // filled by Hibernate
    }
}
