package io.github.omarsherifsayed.tajweed.business.source.entity;

import io.github.omarsherifsayed.tajweed.general.audit.AbstractAuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * One source file of the content (table {@code content_source}): its name, licence, address and SHA-256.
 *
 * <p>Written only by the import migrations, never by the code ({@code @Immutable}). The columns are described in
 * {@code docs/database/README.md}.
 */
@Getter
@Entity
@Immutable
@Table(name = "content_source")
public class ContentSource extends AbstractAuditingEntity {

    /** The key. */
    @Id
    @Column(name = "pk_id")
    private Integer id;

    /** A stable name, for example {@code TANZIL_UTHMANI}. */
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    /** The source's own name and version. */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** The file's path inside the application. */
    @Column(name = "resource_path", nullable = false, length = 300)
    private String resourcePath;

    /** The file's SHA-256, checked by the import. */
    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    /** The file's licence. */
    @Column(name = "licence", nullable = false, length = 100)
    private String licence;

    /** Where the file was obtained. */
    @Column(name = "source_url", nullable = false, length = 300)
    private String sourceUrl;

    /** The day it was obtained. */
    @Column(name = "obtained_on", nullable = false)
    private LocalDate obtainedOn;

    /**
     * For JPA only.
     */
    protected ContentSource() {
        // filled by Hibernate
    }
}
