package io.github.omarsherifsayed.tajweed.business.radio.service;

import io.github.omarsherifsayed.tajweed.business.radio.schema.response.DtoRadios;

/**
 * Reads the radio stations and the health of their streams.
 */
public interface RadioRetrievalService {

    /**
     * Every station.
     *
     * @return the stations, in the list's order, with the credits
     */
    DtoRadios getRadios();

    /**
     * One station.
     *
     * @param stationId the station
     * @return the station alone, with the credits
     */
    DtoRadios getRadio(int stationId);
}
