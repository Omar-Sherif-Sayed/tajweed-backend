package io.github.omarsherifsayed.tajweed.business.reciter.repository;

import io.github.omarsherifsayed.tajweed.business.reciter.entity.Reciter;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reads the reciters.
 */
public interface ReciterRepository extends JpaRepository<Reciter, Short> {

    /**
     * Every reciter, in the order of the catalogue.
     *
     * @return the reciters
     */
    List<Reciter> findAllByOrderByIdAsc();
}
