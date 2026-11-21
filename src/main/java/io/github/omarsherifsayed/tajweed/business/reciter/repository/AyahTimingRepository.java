package io.github.omarsherifsayed.tajweed.business.reciter.repository;

import io.github.omarsherifsayed.tajweed.business.reciter.entity.AyahTiming;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reads the ayah timings.
 */
public interface AyahTimingRepository extends JpaRepository<AyahTiming, Integer> {

    /**
     * The timings of one surah of a recitation, in order.
     *
     * @param recitationId the recitation
     * @param surahId      the surah
     * @return one row per ayah (from 0)
     */
    List<AyahTiming> findByRecitationIdAndSurahIdOrderByAyahNumberAsc(Short recitationId, Short surahId);
}
