package io.github.omarsherifsayed.tajweed.business.hadith.repository;

import io.github.omarsherifsayed.tajweed.business.hadith.entity.HadeethEncCategory;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reads HadeethEnc's categories.
 */
public interface HadeethEncCategoryRepository extends JpaRepository<HadeethEncCategory, Integer> {
}
