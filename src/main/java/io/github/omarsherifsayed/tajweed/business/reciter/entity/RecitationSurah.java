package io.github.omarsherifsayed.tajweed.business.reciter.entity;

import io.github.omarsherifsayed.tajweed.general.audit.AbstractAuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * A surah a recitation holds (table {@code recitation_surah}) — its audio file was checked on the import day.
 */
@Getter
@Entity
@Immutable
@Table(name = "recitation_surah")
public class RecitationSurah extends AbstractAuditingEntity {

    /** The primary key. */
    @Id
    @Column(name = "pk_id")
    private Integer id;

    /** The recitation. */
    @Column(name = "recitation_id")
    private Short recitationId;

    /** The surah. */
    @Column(name = "surah_id")
    private Short surahId;

    /**
     * For JPA only.
     */
    protected RecitationSurah() {
        // filled by Hibernate
    }
}
