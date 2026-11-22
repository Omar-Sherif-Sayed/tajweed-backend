package io.github.omarsherifsayed.tajweed.business.source.repository;

import io.github.omarsherifsayed.tajweed.business.source.entity.ContentSync;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reads and writes the state of the syncs (run-time state).
 */
public interface ContentSyncRepository extends JpaRepository<ContentSync, Integer> {

    /**
     * The state of one sync.
     *
     * @param code what is synced
     * @return its state, empty when it never ran
     */
    Optional<ContentSync> findByCode(String code);
}
