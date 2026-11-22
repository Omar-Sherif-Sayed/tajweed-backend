package io.github.omarsherifsayed.tajweed.business.hadith.entity;

import io.github.omarsherifsayed.tajweed.general.audit.AbstractAuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * A hadith in one language, exactly as HadeethEnc sent it (table {@code hadeethenc_hadith_text}).
 */
@Getter
@Entity
@Immutable
@Table(name = "hadeethenc_hadith_text")
public class HadeethEncHadithText extends AbstractAuditingEntity {

    /** The primary key. */
    @Id
    @Column(name = "pk_id")
    private Integer id;

    /** The hadith. */
    @Column(name = "hadith_id")
    private Integer hadithId;

    /** The language code. */
    @Column(name = "language")
    private String language;

    /** Its title. */
    @Column(name = "title")
    private String title;

    /** The hadith's text. */
    @Column(name = "hadeeth")
    private String hadeeth;

    /** The narrator's introduction, or {@code null}. */
    @Column(name = "hadeeth_intro")
    private String hadeethIntro;

    /** Who reported it («متفق عليه» …), or {@code null}. */
    @Column(name = "attribution")
    private String attribution;

    /** Its grade, as HadeethEnc states it. */
    @Column(name = "grade")
    private String grade;

    /** The explanation, or {@code null}. */
    @Column(name = "explanation")
    private String explanation;

    /** The benefits: the JSON array as received, or {@code null}. */
    @Column(name = "hints")
    private String hints;

    /** The word meanings: the JSON array of {@code {word, meaning}} as received, or {@code null}. */
    @Column(name = "words_meanings")
    private String wordsMeanings;

    /** The references (book, volume, page, number), or {@code null}. */
    @Column(name = "reference")
    private String reference;

    /** The SHA-256 of the answer as received. */
    @Column(name = "sha256")
    private String sha256;

    /** When it was fetched. */
    @Column(name = "fetched_at")
    private Instant fetchedAt;

    /**
     * For JPA only.
     */
    protected HadeethEncHadithText() {
        // filled by Hibernate
    }
}
