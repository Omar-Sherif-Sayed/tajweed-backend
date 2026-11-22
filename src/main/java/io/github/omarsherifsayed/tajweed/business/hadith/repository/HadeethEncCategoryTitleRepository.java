package io.github.omarsherifsayed.tajweed.business.hadith.repository;

import io.github.omarsherifsayed.tajweed.business.hadith.entity.HadeethEncCategoryTitle;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reads the categories' titles.
 */
public interface HadeethEncCategoryTitleRepository extends JpaRepository<HadeethEncCategoryTitle, Integer> {

    /**
     * The titles in some languages.
     *
     * @param languages the language codes
     * @return the titles
     */
    List<HadeethEncCategoryTitle> findByLanguageIn(Collection<String> languages);
}
