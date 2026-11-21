package io.github.omarsherifsayed.tajweed.business.quran.entity;

import io.github.omarsherifsayed.tajweed.business.quran.enums.RevelationType;
import io.github.omarsherifsayed.tajweed.general.audit.AbstractAuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * A surah (table {@code surah}) — its names, revelation type and order, and its number of ayahs, from Tanzil's
 * metadata. Written only by the import migration ({@code @Immutable}).
 */
@Getter
@Entity
@Immutable
@Table(name = "surah")
public class Surah extends AbstractAuditingEntity {

    /** The surah's number, 1 to 114. */
    @Id
    @Column(name = "pk_id")
    private Short id;

    /** Its Arabic name, for example «الفاتحة». */
    @Column(name = "name_ar", nullable = false, length = 50)
    private String nameAr;

    /** Its name in Latin letters, for example "Al-Faatiha". */
    @Column(name = "name_transliteration", nullable = false, length = 50)
    private String nameTransliteration;

    /** The English meaning of its name, for example "The Opening". */
    @Column(name = "name_en", nullable = false, length = 100)
    private String nameEn;

    /** Makki or Madani. */
    @Enumerated(EnumType.STRING)
    @Column(name = "revelation_type", nullable = false, length = 10)
    private RevelationType revelationType;

    /** Its place in the order of revelation. */
    @Column(name = "revelation_order", nullable = false)
    private Short revelationOrder;

    /** How many ayahs it has. */
    @Column(name = "ayah_count", nullable = false)
    private Short ayahCount;

    /**
     * For JPA only.
     */
    protected Surah() {
        // filled by Hibernate
    }
}
