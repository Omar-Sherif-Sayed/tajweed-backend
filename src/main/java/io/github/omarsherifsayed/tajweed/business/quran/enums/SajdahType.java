package io.github.omarsherifsayed.tajweed.business.quran.enums;

/**
 * The type of a place of prostration (sajdah), exactly as Tanzil's metadata gives it for its 15 places.
 *
 * <p>These are Tanzil's own labels; the application does not judge the ruling. The clients name the place with the
 * sign the printed mushaf uses (۩) and their own label.
 */
public enum SajdahType {

    /** Tanzil: "recommended". */
    RECOMMENDED,

    /** Tanzil: "obligatory". */
    OBLIGATORY
}
