package io.github.omarsherifsayed.tajweed.business.hadith.entity;

import io.github.omarsherifsayed.tajweed.general.audit.AbstractAuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * The title of a category in one language, as HadeethEnc writes it (table {@code hadeethenc_category_title}).
 */
@Getter
@Entity
@Immutable
@Table(name = "hadeethenc_category_title")
public class HadeethEncCategoryTitle extends AbstractAuditingEntity {

    /** The primary key. */
    @Id
    @Column(name = "pk_id")
    private Integer id;

    /** The category. */
    @Column(name = "category_id")
    private Integer categoryId;

    /** The language code. */
    @Column(name = "language")
    private String language;

    /** The title, as received. */
    @Column(name = "title")
    private String title;

    /**
     * For JPA only.
     */
    protected HadeethEncCategoryTitle() {
        // filled by Hibernate
    }
}
