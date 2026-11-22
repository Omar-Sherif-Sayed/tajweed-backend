package io.github.omarsherifsayed.tajweed.business.hadith.entity;

import io.github.omarsherifsayed.tajweed.general.audit.AbstractAuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * A category of HadeethEnc (table {@code hadeethenc_category}); the id is theirs.
 */
@Getter
@Entity
@Immutable
@Table(name = "hadeethenc_category")
public class HadeethEncCategory extends AbstractAuditingEntity {

    /** HadeethEnc's id. */
    @Id
    @Column(name = "pk_id")
    private Integer id;

    /** The parent category, or {@code null} for a root. */
    @Column(name = "parent_id")
    private Integer parentId;

    /**
     * For JPA only.
     */
    protected HadeethEncCategory() {
        // filled by Hibernate
    }
}
