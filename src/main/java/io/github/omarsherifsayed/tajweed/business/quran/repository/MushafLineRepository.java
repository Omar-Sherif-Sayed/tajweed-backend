package io.github.omarsherifsayed.tajweed.business.quran.repository;

import io.github.omarsherifsayed.tajweed.business.quran.entity.MushafLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reads the lines of the printed mushaf.
 */
public interface MushafLineRepository extends JpaRepository<MushafLine, Integer> {

    /**
     * The lines of a page, top to bottom.
     *
     * @param page the page, 1 to 604
     * @return its lines (15, or 8 on pages 1 and 2)
     */
    List<MushafLine> findByPageOrderByLineAsc(Short page);
}
