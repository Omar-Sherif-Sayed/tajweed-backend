package io.github.omarsherifsayed.tajweed.business.quran.repository;

/**
 * Where a juz starts — a closed projection of a list query ({@code AGENTS.md} §9).
 */
public interface JuzStartProjection {

    /**
     * The juz.
     *
     * @return its number, 1 to 30
     */
    Short getJuz();

    /**
     * The surah of its first ayah.
     *
     * @return the surah's number
     */
    Short getSurahNumber();

    /**
     * Its first ayah.
     *
     * @return the ayah's number in its surah
     */
    Short getAyahNumber();

    /**
     * The page of its first ayah.
     *
     * @return the page, 1 to 604
     */
    Short getPage();
}
