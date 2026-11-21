package io.github.omarsherifsayed.tajweed.business.reciter.repository;

import io.github.omarsherifsayed.tajweed.business.reciter.entity.AudioSource;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reads the audio sources.
 */
public interface AudioSourceRepository extends JpaRepository<AudioSource, Integer> {

    /**
     * The sources of a recitation, in the order the players try them.
     *
     * @param recitationId the recitation
     * @return its sources
     */
    List<AudioSource> findByRecitationIdOrderByPriorityAsc(Short recitationId);

    /**
     * Every source, for the health check.
     *
     * @return the sources, by id
     */
    List<AudioSource> findAllByOrderByIdAsc();
}
