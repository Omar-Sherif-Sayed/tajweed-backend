package io.github.omarsherifsayed.tajweed.business.hadith.repository;

import io.github.omarsherifsayed.tajweed.business.hadith.entity.HadeethEncHadithText;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reads the hadiths' texts.
 */
public interface HadeethEncHadithTextRepository extends JpaRepository<HadeethEncHadithText, Integer> {

    /**
     * One hadith's texts in some languages.
     *
     * @param hadithId  the hadith
     * @param languages the language codes
     * @return the texts
     */
    List<HadeethEncHadithText> findByHadithIdAndLanguageIn(Integer hadithId, Collection<String> languages);

    /**
     * Several hadiths' texts in some languages.
     *
     * @param hadithIds the hadiths
     * @param languages the language codes
     * @return the texts
     */
    List<HadeethEncHadithText> findByHadithIdInAndLanguageIn(Collection<Integer> hadithIds, Collection<String> languages);
}
