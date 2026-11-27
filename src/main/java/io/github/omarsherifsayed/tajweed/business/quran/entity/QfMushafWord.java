package io.github.omarsherifsayed.tajweed.business.quran.entity;

import io.github.omarsherifsayed.tajweed.general.audit.AbstractAuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * A word (or an ayah's end marker) of the Quran Foundation's mushaf 2, QCF V1 — the King Fahd Complex's 1405H print
 * (table {@code qf_mushaf_word}): the glyph that draws it in its page's QCF V1 font, and where it stands. Written only
 * by {@code QfMushafSync}, from their Content Sync; read-only here. The glyph only draws the text: what is copied and
 * searched stays the Tanzil text ({@code AGENTS.md} §6.1).
 */
@Getter
@Entity
@Immutable
@Table(name = "qf_mushaf_word")
public class QfMushafWord extends AbstractAuditingEntity {

    /** The row's id. */
    @Id
    @Column(name = "pk_id")
    private Integer id;

    /** The Quran Foundation's mushaf id (2 = QCF V1). */
    @Column(name = "mushaf_id", nullable = false)
    private Short mushafId;

    /** Their record's id, the key of their row changes. */
    @Column(name = "qf_word_id", nullable = false)
    private Integer qfWordId;

    /** The ayah ({@code ayah.pk_id}; their verse ids are the same 1 – 6236). */
    @Column(name = "ayah_id", nullable = false)
    private Short ayahId;

    /** Its order on the page, from 1. */
    @Column(name = "position", nullable = false)
    private Short position;

    /** {@code word}, or {@code end} for the ayah's number. */
    @Column(name = "char_type", nullable = false, length = 4)
    private String charType;

    /** The page, 1 to 604 — also the font that draws the glyph. */
    @Column(name = "page", nullable = false)
    private Short page;

    /** The line on the page, 1 to 15. */
    @Column(name = "line", nullable = false)
    private Short line;

    /** The glyph's characters in the page's font, as received. */
    @Column(name = "glyph", nullable = false, length = 8)
    private String glyph;

    /** When it was synced. */
    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    /**
     * For JPA only.
     */
    protected QfMushafWord() {
        // filled by Hibernate
    }
}
