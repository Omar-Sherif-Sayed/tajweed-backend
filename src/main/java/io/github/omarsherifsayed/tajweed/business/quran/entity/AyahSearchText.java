package io.github.omarsherifsayed.tajweed.business.quran.entity;

import io.github.omarsherifsayed.tajweed.general.audit.AbstractAuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * The search text of an ayah (table {@code ayah_search_text}): Tanzil «Simple» and «Simple (Clean)», verbatim. Used only
 * to build the search index — never displayed ({@code AGENTS.md} §8.1).
 */
@Getter
@Entity
@Immutable
@Table(name = "ayah_search_text")
public class AyahSearchText extends AbstractAuditingEntity {

    /** The ayah ({@code ayah.pk_id}). */
    @Id
    @Column(name = "ayah_id")
    private Integer ayahId;

    /** The everyday spelling with tashkeel. */
    @Column(name = "text_simple", nullable = false, columnDefinition = "text")
    private String textSimple;

    /** The everyday spelling without tashkeel. */
    @Column(name = "text_simple_clean", nullable = false, columnDefinition = "text")
    private String textSimpleClean;

    /**
     * For JPA only.
     */
    protected AyahSearchText() {
        // filled by Hibernate
    }
}
