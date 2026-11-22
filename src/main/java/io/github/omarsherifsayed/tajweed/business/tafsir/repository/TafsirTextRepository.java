package io.github.omarsherifsayed.tajweed.business.tafsir.repository;

import io.github.omarsherifsayed.tajweed.business.tafsir.entity.TafsirText;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Reads the tafsirs' texts.
 */
public interface TafsirTextRepository extends JpaRepository<TafsirText, Integer> {

    /**
     * The texts that cover an ayah, in every tafsir — only those with text.
     *
     * @param ayahId the ayah
     * @return the texts
     */
    @Query("select t from TafsirText t where t.ayahId <= :ayahId and t.ayahToId >= :ayahId and t.text <> ''")
    List<TafsirText> findCovering(@Param("ayahId") short ayahId);
}
