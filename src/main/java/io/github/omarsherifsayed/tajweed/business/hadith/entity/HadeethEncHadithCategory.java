package io.github.omarsherifsayed.tajweed.business.hadith.entity;

import io.github.omarsherifsayed.tajweed.general.audit.AbstractAuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * A category a hadith belongs to, as HadeethEnc lists it (table {@code hadeethenc_hadith_category}).
 */
@Getter
@Entity
@Immutable
@Table(name = "hadeethenc_hadith_category")
public class HadeethEncHadithCategory extends AbstractAuditingEntity {

    /** The primary key. */
    @Id
    @Column(name = "pk_id")
    private Integer id;

    /** The hadith. */
    @Column(name = "hadith_id")
    private Integer hadithId;

    /** The category. */
    @Column(name = "category_id")
    private Integer categoryId;

    /**
     * For JPA only.
     */
    protected HadeethEncHadithCategory() {
        // filled by Hibernate
    }
}
