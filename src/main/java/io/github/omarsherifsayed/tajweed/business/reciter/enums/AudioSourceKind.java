package io.github.omarsherifsayed.tajweed.business.reciter.enums;

/**
 * What one file of a source holds.
 */
public enum AudioSourceKind {

    /** A whole surah; per-ayah playback needs the recitation's timings. */
    SURAH_FILE,

    /** One ayah. */
    AYAH_FILE
}
