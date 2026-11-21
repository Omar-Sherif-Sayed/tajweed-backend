package io.github.omarsherifsayed.tajweed.business.reciter.entity;

import io.github.omarsherifsayed.tajweed.general.audit.AbstractAuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * A riwayah (table {@code riwayah}): its name as mp3quran writes it, its rawi and its qari taken from that name,
 * and whether it is the riwayah of the Quran text the application shows (Hafs an Asim). The id is mp3quran's.
 */
@Getter
@Entity
@Immutable
@Table(name = "riwayah")
public class Riwayah extends AbstractAuditingEntity {

    /** The primary key. */
    @Id
    @Column(name = "pk_id")
    private Short id;

    /** The name as mp3quran writes it in Arabic («ورش عن نافع»). */
    @Column(name = "name_ar")
    private String nameAr;

    /** The name as mp3quran writes it in English. */
    @Column(name = "name_en")
    private String nameEn;

    /** The rawi — the part of the name before «عن». */
    @Column(name = "rawi_ar")
    private String rawiAr;

    /** The qari — the part of the name after «عن». */
    @Column(name = "qari_ar")
    private String qariAr;

    /** {@code true} for Hafs an Asim, the riwayah of the shipped Tanzil text; otherwise its recitations are listening only (§6.7). */
    @Column(name = "matches_displayed_text")
    private Boolean matchesDisplayedText;

    /**
     * For JPA only.
     */
    protected Riwayah() {
        // filled by Hibernate
    }
}
