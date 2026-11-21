package io.github.omarsherifsayed.tajweed.business.quran.repository;

import io.github.omarsherifsayed.tajweed.business.quran.entity.Surah;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reads the surahs.
 */
public interface SurahRepository extends JpaRepository<Surah, Short> {

    /**
     * The 114 surahs in the order of the mushaf.
     *
     * @return every surah
     */
    List<Surah> findAllByOrderByIdAsc();
}
