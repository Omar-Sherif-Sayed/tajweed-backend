package io.github.omarsherifsayed.tajweed.business.quran.schema.response;

import io.github.omarsherifsayed.tajweed.business.source.schema.response.DtoContentSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * The answer of a Quran search ({@code AGENTS.md} §8.1, F16): the three kinds of result at once — the ayah a reference
 * names, the surahs a name names, and the ayahs that contain the words.
 *
 * @param query          the query as received
 * @param withTashkeel   whether the query carried tashkeel (then the marks had to agree)
 * @param ayah           the ayah the query names ({@code 2:255}, «البقرة 255»), absent when it names none
 * @param surahs         the surahs the query names, best first
 * @param total          how many ayahs contain the words
 * @param page           the page of results, from 0
 * @param size           results per page
 * @param hits           this page's ayahs, in the order of the mushaf
 * @param countsBySurah  how many of all the found ayahs are in each surah, in order
 * @param textSource     the source of the displayed text (Tanzil Uthmani — shown with its link)
 * @param searchSources  the sources of the search text (Tanzil Simple and Simple Clean)
 */
@Schema(description = "A Quran search: the ayah a reference names, the surahs a name names, and the ayahs with the words.")
public record DtoQuranSearch(String query, boolean withTashkeel, DtoAyah ayah, List<DtoSurah> surahs, int total,
                             int page, int size, List<DtoSearchHit> hits, List<DtoSurahCount> countsBySurah,
                             DtoContentSource textSource, List<DtoContentSource> searchSources) {
}
