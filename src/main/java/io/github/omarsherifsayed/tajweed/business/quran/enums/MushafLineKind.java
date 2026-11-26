package io.github.omarsherifsayed.tajweed.business.quran.enums;

/**
 * What a line of the printed mushaf holds ({@code mushaf_line.kind}).
 */
public enum MushafLineKind {

    /** The decorated band with the surah's name (the clients add «ترتيبها» and «آياتها», as the printed copy does). */
    SURAH_HEADER,

    /** The basmala on its own line, before a surah other than al-Fatihah and at-Tawbah. */
    BASMALA,

    /** A run of the Tanzil text's words. */
    TEXT
}
