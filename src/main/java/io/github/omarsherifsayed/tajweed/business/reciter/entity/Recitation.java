package io.github.omarsherifsayed.tajweed.business.reciter.entity;

import io.github.omarsherifsayed.tajweed.business.reciter.enums.AudioProvider;
import io.github.omarsherifsayed.tajweed.business.reciter.enums.RecitationStyle;
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
 * A recitation (table {@code recitation}): one reciter, one riwayah, one style, a set of surahs and its audio
 * sources. Its name is the source's own label («حفص عن عاصم - مرتل»).
 */
@Getter
@Entity
@Immutable
@Table(name = "recitation")
public class Recitation extends AbstractAuditingEntity {

    /** The primary key. */
    @Id
    @Column(name = "pk_id")
    private Short id;

    /** The reciter. */
    @Column(name = "reciter_id")
    private Short reciterId;

    /** The riwayah. */
    @Column(name = "riwayah_id")
    private Short riwayahId;

    /** The style, or {@code null} when the source does not state it. */
    @Enumerated(EnumType.STRING)
    @Column(name = "style")
    private RecitationStyle style;

    /** The source's label in Arabic. */
    @Column(name = "name_ar")
    private String nameAr;

    /** The source's label in English. */
    @Column(name = "name_en")
    private String nameEn;

    /** Who serves its first source: mp3quran (surah files) or EveryAyah (ayah files). */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider")
    private AudioProvider provider;

    /** Its moshaf id on mp3quran.net, or {@code null} for an EveryAyah-only recitation. */
    @Column(name = "mp3quran_moshaf_id")
    private Integer mp3quranMoshafId;

    /** How many surahs it holds (114 when complete). */
    @Column(name = "surah_count")
    private Short surahCount;

    /** Whether ayah timings were imported for it. */
    @Column(name = "has_timings")
    private Boolean hasTimings;

    /**
     * For JPA only.
     */
    protected Recitation() {
        // filled by Hibernate
    }
}
