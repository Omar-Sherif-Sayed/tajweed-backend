package io.github.omarsherifsayed.tajweed.business.radio.service.impl;

import io.github.omarsherifsayed.tajweed.business.radio.entity.RadioStation;
import io.github.omarsherifsayed.tajweed.business.radio.entity.RadioStream;
import io.github.omarsherifsayed.tajweed.business.radio.entity.RadioStreamCheck;
import io.github.omarsherifsayed.tajweed.business.radio.mapper.RadioMapper;
import io.github.omarsherifsayed.tajweed.business.radio.repository.RadioStationRepository;
import io.github.omarsherifsayed.tajweed.business.radio.repository.RadioStreamCheckRepository;
import io.github.omarsherifsayed.tajweed.business.radio.repository.RadioStreamRepository;
import io.github.omarsherifsayed.tajweed.business.radio.schema.response.DtoRadioStation;
import io.github.omarsherifsayed.tajweed.business.radio.schema.response.DtoRadios;
import io.github.omarsherifsayed.tajweed.business.radio.service.RadioRetrievalService;
import io.github.omarsherifsayed.tajweed.business.reciter.entity.Riwayah;
import io.github.omarsherifsayed.tajweed.business.reciter.mapper.ReciterMapper;
import io.github.omarsherifsayed.tajweed.business.reciter.repository.RiwayahRepository;
import io.github.omarsherifsayed.tajweed.business.source.service.ContentSourceRetrievalService;
import io.github.omarsherifsayed.tajweed.general.exception.BaseException;
import io.github.omarsherifsayed.tajweed.general.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the imported radio stations and the health of their streams.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RadioRetrievalServiceImpl implements RadioRetrievalService {

    /** The code of the mp3quran radio list's source row. */
    static final String MP3QURAN_RADIOS = "MP3QURAN_RADIOS";

    /** Reads the stations. */
    private final RadioStationRepository radioStationRepository;

    /** Reads the streams. */
    private final RadioStreamRepository radioStreamRepository;

    /** Reads the streams' health. */
    private final RadioStreamCheckRepository radioStreamCheckRepository;

    /** Reads the riwayat. */
    private final RiwayahRepository riwayahRepository;

    /** Gives the credits. */
    private final ContentSourceRetrievalService contentSourceRetrievalService;

    /**
     * {@inheritDoc}
     *
     * @return the stations
     */
    @Override
    public DtoRadios getRadios() {
        Map<Short, List<RadioStream>> streams = radioStreamRepository.findAllByOrderByStationIdAscPriorityAsc().stream()
                .collect(Collectors.groupingBy(RadioStream::getStationId));
        Map<Integer, RadioStreamCheck> checks = checks();
        Map<Short, Riwayah> riwayat = riwayat();
        List<DtoRadioStation> list = radioStationRepository.findAllByOrderBySortOrderAsc().stream()
                .map(s -> toDto(s, streams.getOrDefault(s.getId(), List.of()), checks, riwayat))
                .toList();
        return new DtoRadios(list, List.of(contentSourceRetrievalService.getSource(MP3QURAN_RADIOS)));
    }

    /**
     * {@inheritDoc}
     *
     * @param stationId the station
     * @return the station
     */
    @Override
    public DtoRadios getRadio(int stationId) {
        if (stationId < 1 || stationId > Short.MAX_VALUE) {
            throw new BaseException(ErrorCode.NOT_FOUND_RADIO);
        }
        RadioStation station = radioStationRepository.findById((short) stationId)
                .orElseThrow(() -> new BaseException(ErrorCode.NOT_FOUND_RADIO));
        DtoRadioStation dto = toDto(station, radioStreamRepository.findByStationIdOrderByPriorityAsc(station.getId()),
                checks(), riwayat());
        return new DtoRadios(List.of(dto), List.of(contentSourceRetrievalService.getSource(MP3QURAN_RADIOS)));
    }

    /**
     * A station with its streams and their health.
     *
     * @param station the station
     * @param streams its streams, in order
     * @param checks  every stream's health, by stream id
     * @param riwayat every riwayah, by id
     * @return the response object
     */
    private static DtoRadioStation toDto(RadioStation station, List<RadioStream> streams,
                                         Map<Integer, RadioStreamCheck> checks, Map<Short, Riwayah> riwayat) {
        Riwayah riwayah = station.getRiwayahId() == null ? null : riwayat.get(station.getRiwayahId());
        return RadioMapper.toDto(station, riwayah == null ? null : ReciterMapper.toDto(riwayah),
                streams.stream().map(s -> RadioMapper.toDto(s, checks.get(s.getId()))).toList());
    }

    /**
     * Every stream's health.
     *
     * @return the checks, by stream id
     */
    private Map<Integer, RadioStreamCheck> checks() {
        return radioStreamCheckRepository.findAll().stream()
                .collect(Collectors.toMap(RadioStreamCheck::getId, Function.identity()));
    }

    /**
     * Every riwayah, by id.
     *
     * @return the riwayat
     */
    private Map<Short, Riwayah> riwayat() {
        return riwayahRepository.findAll().stream().collect(Collectors.toMap(Riwayah::getId, Function.identity()));
    }
}
