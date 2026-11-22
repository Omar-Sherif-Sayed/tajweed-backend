package io.github.omarsherifsayed.tajweed.business.radio.entity;

import io.github.omarsherifsayed.tajweed.business.radio.enums.RadioKind;
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
 * A radio station (table {@code radio_station}): its names as the source writes them, what it broadcasts, and — when
 * the channel is one of our reciters or names its riwayah — the links to them.
 */
@Getter
@Entity
@Immutable
@Table(name = "radio_station")
public class RadioStation extends AbstractAuditingEntity {

    /** The primary key. */
    @Id
    @Column(name = "pk_id")
    private Short id;

    /** Who lists the station ({@code MP3QURAN}). */
    @Column(name = "provider")
    private String provider;

    /** The provider's id of the channel. */
    @Column(name = "provider_key")
    private Integer providerKey;

    /** What it broadcasts. */
    @Enumerated(EnumType.STRING)
    @Column(name = "kind")
    private RadioKind kind;

    /** Our reciter, when the channel is one of the reciters of our catalogue; otherwise {@code null}. */
    @Column(name = "reciter_id")
    private Short reciterId;

    /** The riwayah, only when the channel names it; otherwise {@code null}. */
    @Column(name = "riwayah_id")
    private Short riwayahId;

    /** The name in Arabic, as the provider writes it. */
    @Column(name = "name_ar")
    private String nameAr;

    /** The name in English, as the provider writes it. */
    @Column(name = "name_en")
    private String nameEn;

    /** The country of a national station (ISO 3166-1 alpha-2); {@code null} otherwise. */
    @Column(name = "country_code")
    private String countryCode;

    /** The broadcaster's own page, for a national station; {@code null} otherwise. */
    @Column(name = "broadcaster_url")
    private String broadcasterUrl;

    /** The place in the list. */
    @Column(name = "sort_order")
    private Short sortOrder;

    /**
     * For JPA only.
     */
    protected RadioStation() {
        // filled by Hibernate
    }
}
