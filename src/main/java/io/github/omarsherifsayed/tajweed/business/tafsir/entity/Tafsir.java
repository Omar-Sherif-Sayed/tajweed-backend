package io.github.omarsherifsayed.tajweed.business.tafsir.entity;

import io.github.omarsherifsayed.tajweed.general.audit.AbstractAuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * A tafsir (table {@code tafsir}): its names as the provider gives them; the id is the provider's.
 */
@Getter
@Entity
@Immutable
@Table(name = "tafsir")
public class Tafsir extends AbstractAuditingEntity {

    /** The provider's resource id. */
    @Id
    @Column(name = "pk_id")
    private Integer id;

    /** {@code QURAN_FOUNDATION}. */
    @Column(name = "provider")
    private String provider;

    /** Its name in Arabic, as the provider gives it, or {@code null}. */
    @Column(name = "name_ar")
    private String nameAr;

    /** Its name in English, as the provider gives it. */
    @Column(name = "name_en")
    private String nameEn;

    /** The author as the provider's Arabic list gives it, or {@code null}. */
    @Column(name = "author_ar")
    private String authorAr;

    /** The author as the provider's English list gives it, or {@code null}. */
    @Column(name = "author_en")
    private String authorEn;

    /** The language of the text, as the provider names it. */
    @Column(name = "language")
    private String language;

    /** The place in the list (our choice, {@code content/reviewed/tafsirs.csv}). */
    @Column(name = "sort_order")
    private Short sortOrder;

    /**
     * For JPA only.
     */
    protected Tafsir() {
        // filled by Hibernate
    }
}
