package io.github.omarsherifsayed.tajweed.business.reciter.repository;

import io.github.omarsherifsayed.tajweed.business.reciter.entity.AudioSourceCheck;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reads and writes the health of the audio sources (run-time state).
 */
public interface AudioSourceCheckRepository extends JpaRepository<AudioSourceCheck, Integer> {
}
