package io.github.omarsherifsayed.tajweed.business.radio.repository;

import io.github.omarsherifsayed.tajweed.business.radio.entity.RadioStreamCheck;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reads and writes the health of the radio streams (run-time state).
 */
public interface RadioStreamCheckRepository extends JpaRepository<RadioStreamCheck, Integer> {
}
