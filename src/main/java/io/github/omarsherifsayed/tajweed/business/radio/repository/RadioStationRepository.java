package io.github.omarsherifsayed.tajweed.business.radio.repository;

import io.github.omarsherifsayed.tajweed.business.radio.entity.RadioStation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reads the radio stations.
 */
public interface RadioStationRepository extends JpaRepository<RadioStation, Short> {

    /**
     * Every station, in the list's order.
     *
     * @return the stations
     */
    List<RadioStation> findAllByOrderBySortOrderAsc();
}
