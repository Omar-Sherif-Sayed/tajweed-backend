package io.github.omarsherifsayed.tajweed.business.quran.repository;

import io.github.omarsherifsayed.tajweed.business.quran.entity.Ayah;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Reads the ayahs, and where each surah and juz starts.
 */
public interface AyahRepository extends JpaRepository<Ayah, Integer> {

    /**
     * The ayahs of a surah, in order.
     *
     * @param surahId the surah's number
     * @return its ayahs
     */
    List<Ayah> findBySurahIdOrderByNumberInSurahAsc(Short surahId);

    /**
     * One ayah.
     *
     * @param surahId       the surah's number
     * @param numberInSurah the ayah's number in it
     * @return the ayah, if the surah has it
     */
    Optional<Ayah> findBySurahIdAndNumberInSurah(Short surahId, Short numberInSurah);

    /**
     * The ayahs on a page of the mushaf, in order.
     *
     * @param page the page, 1 to 604
     * @return its ayahs (an ayah that runs over two pages belongs to the page where it starts)
     */
    List<Ayah> findByPageOrderByIdAsc(Short page);

    /*
     * The aliases are quoted: PostgreSQL would otherwise lower-case them, and the projections' getters would not match.
     */

    /**
     * The page on which each surah starts.
     *
     * @return one row per surah, in order
     */
    @Query(value = "select surah_id as \"surahNumber\", min(page) as \"startPage\" from ayah group by surah_id "
            + "order by surah_id", nativeQuery = true)
    List<SurahStartProjection> findSurahStarts();

    /**
     * Where each juz starts.
     *
     * @return one row per juz, in order: the first ayah and its page
     */
    @Query(value = "select distinct on (juz) juz as \"juz\", surah_id as \"surahNumber\", number_in_surah as \"ayahNumber\", "
            + "page as \"page\" from ayah order by juz, pk_id", nativeQuery = true)
    List<JuzStartProjection> findJuzStarts();
}
