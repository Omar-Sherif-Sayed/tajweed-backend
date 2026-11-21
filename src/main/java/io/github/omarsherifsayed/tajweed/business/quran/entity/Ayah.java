package io.github.omarsherifsayed.tajweed.business.quran.entity;

import io.github.omarsherifsayed.tajweed.business.quran.enums.SajdahType;
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
 * An ayah (table {@code ayah}): its text, byte for byte as Tanzil gives it, and its place in the printed mushaf —
 * page, juz, hizb quarter, sajdah. Written only by the import migration; the code never changes it
 * ({@code @Immutable}, {@code AGENTS.md} §6.1).
 */
@Getter
@Entity
@Immutable
@Table(name = "ayah")
public class Ayah extends AbstractAuditingEntity {

    /** The ayah's place in the mushaf, 1 to 6,236. */
    @Id
    @Column(name = "pk_id")
    private Integer id;

    /** The number of its surah. */
    @Column(name = "surah_id", nullable = false)
    private Short surahId;

    /** Its number in the surah. */
    @Column(name = "number_in_surah", nullable = false)
    private Short numberInSurah;

    /** The Tanzil Uthmani text, never edited. */
    @Column(name = "text_uthmani", nullable = false, columnDefinition = "text")
    private String textUthmani;

    /** 4 when the text begins with the basmala (ayah 1 of every surah except 1 and 9), otherwise 0. */
    @Column(name = "basmala_word_count", nullable = false)
    private Short basmalaWordCount;

    /** Its page in the Madinah mushaf, 1 to 604. */
    @Column(name = "page", nullable = false)
    private Short page;

    /** Its juz, 1 to 30. */
    @Column(name = "juz", nullable = false)
    private Short juz;

    /** Its hizb quarter, 1 to 240 (the hizb is (quarter − 1) / 4 + 1). */
    @Column(name = "hizb_quarter", nullable = false)
    private Short hizbQuarter;

    /** The type of its place of prostration, or {@code null} when it has none. */
    @Enumerated(EnumType.STRING)
    @Column(name = "sajdah_type", length = 15)
    private SajdahType sajdahType;

    /**
     * For JPA only.
     */
    protected Ayah() {
        // filled by Hibernate
    }
}
