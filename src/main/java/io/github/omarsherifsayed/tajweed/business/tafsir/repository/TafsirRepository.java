package io.github.omarsherifsayed.tajweed.business.tafsir.repository;

import io.github.omarsherifsayed.tajweed.business.tafsir.entity.Tafsir;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reads the tafsirs.
 */
public interface TafsirRepository extends JpaRepository<Tafsir, Integer> {

    /**
     * Every tafsir, in the list's order.
     *
     * @return the tafsirs
     */
    List<Tafsir> findAllByOrderBySortOrderAsc();
}
