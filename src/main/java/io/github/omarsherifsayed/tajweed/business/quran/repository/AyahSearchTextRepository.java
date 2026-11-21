package io.github.omarsherifsayed.tajweed.business.quran.repository;

import io.github.omarsherifsayed.tajweed.business.quran.entity.AyahSearchText;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reads the search text (once, to build the search index).
 */
public interface AyahSearchTextRepository extends JpaRepository<AyahSearchText, Integer> {
}
