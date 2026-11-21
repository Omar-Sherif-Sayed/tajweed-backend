package io.github.omarsherifsayed.tajweed.business.reciter.entity;

import io.github.omarsherifsayed.tajweed.general.audit.AbstractAuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * When an ayah starts and ends in a surah file (table {@code ayah_timing}), from mp3quran's timings; ayah 0
 * is what is recited before ayah 1 (the isti'adha, and the basmala where it is not ayah 1).
 */
@Getter
@Entity
@Immutable
@Table(name = "ayah_timing")
public class AyahTiming extends AbstractAuditingEntity {

    /** The primary key. */
    @Id
    @Column(name = "pk_id")
    private Integer id;

    /** The recitation. */
    @Column(name = "recitation_id")
    private Short recitationId;

    /** The surah. */
    @Column(name = "surah_id")
    private Short surahId;

    /** The ayah, from 0. */
    @Column(name = "ayah_number")
    private Short ayahNumber;

    /** Its start in the surah file, in milliseconds. */
    @Column(name = "start_ms")
    private Integer startMs;

    /** Its end, in milliseconds. */
    @Column(name = "end_ms")
    private Integer endMs;

    /**
     * For JPA only.
     */
    protected AyahTiming() {
        // filled by Hibernate
    }
}
