package io.github.omarsherifsayed.tajweed.business.quran.enums;

/**
 * Where a surah was revealed, as Tanzil's metadata classifies it ({@code type}: Meccan / Medinan).
 *
 * <p>The clients show the label in the user's language from their own message files (Makki / Madani — «مكية» /
 * «مدنية»). ☐ {@code P0-12}: the classification is compared with the printed Madinah mushaf's index, surah by surah —
 * sources differ for a few surahs ({@code AGENTS.md} §8.1).
 */
public enum RevelationType {

    /** Revealed in Makkah (Tanzil: "Meccan"). */
    MECCAN,

    /** Revealed in Madinah (Tanzil: "Medinan"). */
    MEDINAN
}
