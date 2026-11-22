package io.github.omarsherifsayed.tajweed.business.hadith.entity;

import io.github.omarsherifsayed.tajweed.general.audit.AbstractAuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * A hadith of HadeethEnc (table {@code hadeethenc_hadith}); the id is theirs.
 */
@Getter
@Entity
@Immutable
@Table(name = "hadeethenc_hadith")
public class HadeethEncHadith extends AbstractAuditingEntity {

    /** HadeethEnc's id. */
    @Id
    @Column(name = "pk_id")
    private Integer id;

    /** When it was last seen in HadeethEnc's list. */
    @Column(name = "listed_at")
    private Instant listedAt;

    /**
     * For JPA only.
     */
    protected HadeethEncHadith() {
        // filled by Hibernate
    }
}
