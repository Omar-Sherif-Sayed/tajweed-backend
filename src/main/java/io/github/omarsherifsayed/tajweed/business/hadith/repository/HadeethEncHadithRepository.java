package io.github.omarsherifsayed.tajweed.business.hadith.repository;

import io.github.omarsherifsayed.tajweed.business.hadith.entity.HadeethEncHadith;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reads HadeethEnc's hadiths.
 */
public interface HadeethEncHadithRepository extends JpaRepository<HadeethEncHadith, Integer> {
}
