package io.github.omarsherifsayed.tajweed.business.quran.entity;

import io.github.omarsherifsayed.tajweed.business.quran.enums.MushafLineKind;
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
 * A line of the printed Madinah mushaf, 1405H (table {@code mushaf_line}): a surah header, the basmala, or a run of the
 * Tanzil text's words given by position — never text of its own ({@code AGENTS.md} §6.1, §8.1). Written only by
 * {@code V1_7_1__ImportMushafLines}.
 */
@Getter
@Entity
@Immutable
@Table(name = "mushaf_line")
public class MushafLine extends AbstractAuditingEntity {

    /** The row's id. */
    @Id
    @Column(name = "pk_id")
    private Integer id;

    /** The page, 1 to 604. */
    @Column(name = "page", nullable = false)
    private Short page;

    /** The line on the page, 1 to 15. */
    @Column(name = "line", nullable = false)
    private Short line;

    /** What the line holds. */
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 12)
    private MushafLineKind kind;

    /** The surah of a header or a basmala; {@code null} on a text line. */
    @Column(name = "surah_number")
    private Short surahNumber;

    /** A text line's first ayah ({@code ayah.pk_id}). */
    @Column(name = "first_ayah_id")
    private Integer firstAyahId;

    /** Its first word: the index, from 0, of the space-separated words of the first ayah's text. */
    @Column(name = "first_word")
    private Short firstWord;

    /** A text line's last ayah ({@code ayah.pk_id}). */
    @Column(name = "last_ayah_id")
    private Integer lastAyahId;

    /** Its last word, counted the same way — the pause marks after the last word belong to the line. */
    @Column(name = "last_word")
    private Short lastWord;

    /**
     * For JPA only.
     */
    protected MushafLine() {
        // filled by Hibernate
    }
}
