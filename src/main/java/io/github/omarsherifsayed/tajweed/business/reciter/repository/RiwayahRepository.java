package io.github.omarsherifsayed.tajweed.business.reciter.repository;

import io.github.omarsherifsayed.tajweed.business.reciter.entity.Riwayah;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reads the riwayat.
 */
public interface RiwayahRepository extends JpaRepository<Riwayah, Short> {
}
