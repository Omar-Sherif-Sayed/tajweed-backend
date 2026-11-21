package io.github.omarsherifsayed.tajweed.business.source.repository;

import io.github.omarsherifsayed.tajweed.business.source.entity.ContentSource;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reads the content sources.
 */
public interface ContentSourceRepository extends JpaRepository<ContentSource, Integer> {

    /**
     * All sources, in the order they were imported.
     *
     * @return every source
     */
    List<ContentSource> findAllByOrderByIdAsc();

    /**
     * One source by its code.
     *
     * @param code the source's stable code, for example {@code TANZIL_UTHMANI}
     * @return the source, if it was imported
     */
    Optional<ContentSource> findByCode(String code);
}
