package io.github.omarsherifsayed.tajweed.business.quran.repository;

/**
 * The page on which a surah starts — a closed projection of a list query ({@code AGENTS.md} §9).
 */
public interface SurahStartProjection {

    /**
     * The surah.
     *
     * @return its number
     */
    Short getSurahNumber();

    /**
     * Where it starts.
     *
     * @return the page of its first ayah
     */
    Short getStartPage();
}
