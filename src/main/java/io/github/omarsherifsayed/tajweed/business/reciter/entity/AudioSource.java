package io.github.omarsherifsayed.tajweed.business.reciter.entity;

import io.github.omarsherifsayed.tajweed.business.reciter.enums.AudioProvider;
import io.github.omarsherifsayed.tajweed.business.reciter.enums.AudioSourceKind;
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
 * An audio source of a recitation (table {@code audio_source}): where its files are, in the order the players
 * try them (F11). A file's address is {@code urlTemplate} with {@code {surah}} and {@code {ayah}} written as three digits.
 */
@Getter
@Entity
@Immutable
@Table(name = "audio_source")
public class AudioSource extends AbstractAuditingEntity {

    /** The primary key. */
    @Id
    @Column(name = "pk_id")
    private Integer id;

    /** The recitation. */
    @Column(name = "recitation_id")
    private Short recitationId;

    /** 1 is tried first. */
    @Column(name = "priority")
    private Short priority;

    /** Surah files or ayah files. */
    @Enumerated(EnumType.STRING)
    @Column(name = "kind")
    private AudioSourceKind kind;

    /** Who serves it. */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider")
    private AudioProvider provider;

    /** The address of a file, with {@code {surah}} and {@code {ayah}}. */
    @Column(name = "url_template")
    private String urlTemplate;

    /** The bitrate the source states, or {@code null}. */
    @Column(name = "bitrate_kbps")
    private Short bitrateKbps;

    /** Whether a client may keep the files for offline listening (F28) — false until the source allows it in writing. */
    @Column(name = "download_allowed")
    private Boolean downloadAllowed;

    /**
     * For JPA only.
     */
    protected AudioSource() {
        // filled by Hibernate
    }
}
