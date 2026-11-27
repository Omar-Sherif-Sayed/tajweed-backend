package io.github.omarsherifsayed.tajweed.business.quran.repository;

import io.github.omarsherifsayed.tajweed.business.quran.entity.QfMushafWord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reads the QCF V1 glyphs of the printed mushaf.
 */
public interface QfMushafWordRepository extends JpaRepository<QfMushafWord, Integer> {

    /**
     * The glyphs of a page, in their order.
     *
     * @param mushafId the Quran Foundation's mushaf id
     * @param page     the page, 1 to 604
     * @return its glyphs, or none when the mushaf is not synced
     */
    List<QfMushafWord> findByMushafIdAndPageOrderByPositionAsc(Short mushafId, Short page);

    /**
     * The glyphs of an ayah on a page, in their order — the basmala is ayah 1:1 on page 1.
     *
     * @param mushafId the Quran Foundation's mushaf id
     * @param ayahId   the ayah
     * @param charType {@code word} or {@code end}
     * @return its glyphs
     */
    List<QfMushafWord> findByMushafIdAndAyahIdAndCharTypeOrderByPositionAsc(Short mushafId, Short ayahId,
                                                                             String charType);
}
