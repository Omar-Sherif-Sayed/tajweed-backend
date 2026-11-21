package io.github.omarsherifsayed.tajweed.business.reciter.enums;

/**
 * Who serves an audio source. The clients stream from it directly — never through our server.
 */
public enum AudioProvider {

    /** mp3quran.net — one file per surah ({@code NNN.mp3}). */
    MP3QURAN,

    /** everyayah.com — one file per ayah ({@code SSSAAA.mp3}); streaming only until EveryAyah answers our letter. */
    EVERYAYAH
}
