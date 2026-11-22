package io.github.omarsherifsayed.tajweed.business.radio.entity;

import io.github.omarsherifsayed.tajweed.general.audit.AbstractAuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * A stream of a radio station (table {@code radio_stream}), in the order the players try them (F11).
 */
@Getter
@Entity
@Immutable
@Table(name = "radio_stream")
public class RadioStream extends AbstractAuditingEntity {

    /** The primary key. */
    @Id
    @Column(name = "pk_id")
    private Integer id;

    /** The station. */
    @Column(name = "station_id")
    private Short stationId;

    /** The stream's address, as the provider publishes it. */
    @Column(name = "url")
    private String url;

    /** 1 is tried first. */
    @Column(name = "priority")
    private Short priority;

    /**
     * For JPA only.
     */
    protected RadioStream() {
        // filled by Hibernate
    }
}
