package io.github.omarsherifsayed.tajweed.business.reciter.repository;

import io.github.omarsherifsayed.tajweed.business.reciter.entity.RecitationSurah;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reads which surahs a recitation holds.
 */
public interface RecitationSurahRepository extends JpaRepository<RecitationSurah, Integer> {

    /**
     * The surahs of a recitation, in order.
     *
     * @param recitationId the recitation
     * @return its surahs
     */
    List<RecitationSurah> findByRecitationIdOrderBySurahIdAsc(Short recitationId);

    /**
     * Whether a recitation holds a surah.
     *
     * @param recitationId the recitation
     * @param surahId      the surah
     * @return {@code true} when it does
     */
    boolean existsByRecitationIdAndSurahId(Short recitationId, Short surahId);
}
