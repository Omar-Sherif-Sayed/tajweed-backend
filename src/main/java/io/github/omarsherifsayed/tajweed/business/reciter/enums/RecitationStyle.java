package io.github.omarsherifsayed.tajweed.business.reciter.enums;

/**
 * The style of a recitation, as its source names it; {@code null} in a recitation whose source does not state it
 * (al-Minshawi's 1967 recording; some EveryAyah folders).
 */
public enum RecitationStyle {

    /** مرتل — read at a measured pace. */
    MURATTAL,

    /** مجود — «المصحف المجود». */
    MUJAWWAD,

    /** معلم — «المصحف المعلم», the teaching recitation (F4). */
    MUALLIM
}
