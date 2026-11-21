package io.github.omarsherifsayed.tajweed.business.reciter.repository;

import io.github.omarsherifsayed.tajweed.business.reciter.entity.Recitation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reads the recitations.
 */
public interface RecitationRepository extends JpaRepository<Recitation, Short> {

    /**
     * Every recitation, in the order of the catalogue.
     *
     * @return the recitations
     */
    List<Recitation> findAllByOrderByIdAsc();

    /**
     * The recitations of one reciter, in the order of the catalogue.
     *
     * @param reciterId the reciter
     * @return his recitations
     */
    List<Recitation> findByReciterIdOrderByIdAsc(Short reciterId);
}
