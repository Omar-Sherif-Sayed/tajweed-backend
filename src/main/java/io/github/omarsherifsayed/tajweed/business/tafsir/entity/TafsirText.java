package io.github.omarsherifsayed.tajweed.business.tafsir.entity;

import io.github.omarsherifsayed.tajweed.general.audit.AbstractAuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * A text of a tafsir (table {@code tafsir_text}), covering one ayah or a range, exactly as received.
 */
@Getter
@Entity
@Immutable
@Table(name = "tafsir_text")
public class TafsirText extends AbstractAuditingEntity {

    /** The primary key. */
    @Id
    @Column(name = "pk_id")
    private Integer id;

    /** The tafsir. */
    @Column(name = "tafsir_id")
    private Integer tafsirId;

    /** The provider's record id. */
    @Column(name = "source_record_id")
    private Integer sourceRecordId;

    /** The first ayah it covers. */
    @Column(name = "ayah_id")
    private Short ayahId;

    /** The last ayah it covers. */
    @Column(name = "ayah_to_id")
    private Short ayahToId;

    /** The text as received (HTML); empty when the provider has none for these ayahs. */
    @Column(name = "text")
    private String text;

    /** When it was received. */
    @Column(name = "synced_at")
    private Instant syncedAt;

    /**
     * For JPA only.
     */
    protected TafsirText() {
        // filled by Hibernate
    }
}
