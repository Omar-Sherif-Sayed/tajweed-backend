package io.github.omarsherifsayed.tajweed.business.radio.repository;

import io.github.omarsherifsayed.tajweed.business.radio.entity.RadioStream;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reads the radio streams.
 */
public interface RadioStreamRepository extends JpaRepository<RadioStream, Integer> {

    /**
     * Every stream, by station and priority.
     *
     * @return the streams
     */
    List<RadioStream> findAllByOrderByStationIdAscPriorityAsc();

    /**
     * The streams of one station, in the order to try them.
     *
     * @param stationId the station
     * @return its streams
     */
    List<RadioStream> findByStationIdOrderByPriorityAsc(Short stationId);
}
