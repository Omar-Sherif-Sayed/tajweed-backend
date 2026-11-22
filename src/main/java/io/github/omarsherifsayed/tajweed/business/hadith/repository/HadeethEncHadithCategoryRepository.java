package io.github.omarsherifsayed.tajweed.business.hadith.repository;

import io.github.omarsherifsayed.tajweed.business.hadith.entity.HadeethEncHadithCategory;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reads which categories each hadith belongs to.
 */
public interface HadeethEncHadithCategoryRepository extends JpaRepository<HadeethEncHadithCategory, Integer> {

    /**
     * The categories of one hadith.
     *
     * @param hadithId the hadith
     * @return its links
     */
    List<HadeethEncHadithCategory> findByHadithIdOrderByCategoryIdAsc(Integer hadithId);
}
