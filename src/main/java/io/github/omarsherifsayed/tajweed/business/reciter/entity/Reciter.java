package io.github.omarsherifsayed.tajweed.business.reciter.entity;

import io.github.omarsherifsayed.tajweed.general.audit.AbstractAuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * A reciter (table {@code reciter}), with his names as mp3quran writes them.
 */
@Getter
@Entity
@Immutable
@Table(name = "reciter")
public class Reciter extends AbstractAuditingEntity {

    /** The primary key. */
    @Id
    @Column(name = "pk_id")
    private Short id;

    /** His name in Arabic. */
    @Column(name = "name_ar")
    private String nameAr;

    /** His name in English. */
    @Column(name = "name_en")
    private String nameEn;

    /** His id on mp3quran.net. */
    @Column(name = "mp3quran_id")
    private Integer mp3quranId;

    /**
     * For JPA only.
     */
    protected Reciter() {
        // filled by Hibernate
    }
}
